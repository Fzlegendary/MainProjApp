package com.example.mainproj.messaging;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.BaseActivity;
import com.example.mainproj.R;
import com.example.mainproj.userSystem.Login_activity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashSet;
import java.util.Set;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends BaseActivity {

    private DrawerLayout drawerLayout;
    private Toolbar chatToolbar;

    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private ArrayList<MessageModel> messagesList;
    private EditText messageInput;
    private MaterialButton sendButton;

    private final String chatId = initChatId();
    private String partnerUsername;
    private String currentUsername;

    private String initChatId() {
        try {
            Intent intent = getIntent();
            if (intent != null) {
                String id = intent.getStringExtra("chatId");
                if (id != null) {
                    return id;
                }
            }
            throw new IllegalStateException("No chat ID provided");
        } catch (Exception e) {
            return null;
        }
    }

    private DatabaseReference messagesRef;
    private ChildEventListener messagesListener;

    private ImageView profileImage;
    private TextView profileName, profileUsername, profileEmail;
    private GoogleSignInClient mGoogleSignInClient;
    private ChatCacheManager cacheManager;
    private Set<Long> loadedTimestamps = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        drawerLayout = findViewById(R.id.chatDrawerLayout);
        chatToolbar  = findViewById(R.id.chatToolbar);
        setSupportActionBar(chatToolbar);

        partnerUsername = getIntent().getStringExtra("username");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(partnerUsername);
        }

        chatToolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(Gravity.END)
        );

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        messagesList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messagesList, this);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);

        cacheManager = ChatCacheManager.getInstance(this);
        loadCachedMessages();

        loadCurrentUsername();

        if (chatId != null) {
            ChatCacheApplication.setCurrentOpenChatId(chatId);
        }

        // ✅ CHANGED: back press handles RIGHT drawer
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(Gravity.END)) {
                    drawerLayout.closeDrawer(Gravity.END);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // ─── Profile drawer setup ─────────────────────────────

        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        profileUsername = findViewById(R.id.profileUsername);
        profileEmail = findViewById(R.id.profileEmail);

        loadUserProfile();

        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            mGoogleSignInClient.signOut();

            Intent intent = new Intent(ChatActivity.this, Login_activity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        sendButton.setOnClickListener(v -> sendMessage());

        if (chatId == null) {
            Toast.makeText(this, "Error: No chat ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        messagesRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages");
        messagesListener = buildMessagesListener();
    }

    private void loadCurrentUsername() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String safeEmail = user.getEmail().replace(".", "_");
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(safeEmail);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUsername = snapshot.child("username").getValue(String.class);
                if (currentUsername != null) {
                    ChatCacheApplication.setCurrentUsername(currentUsername);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty() || chatId == null || currentUsername == null) return;

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", text);
        messageData.put("senderUsername", currentUsername);
        messageData.put("timestamp", ServerValue.TIMESTAMP);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        messageData.put("time", sdf.format(new Date()));

        messagesRef.push().setValue(messageData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                messageInput.setText("");

                Map<String, Object> lastMessageUpdate = new HashMap<>();
                lastMessageUpdate.put("lastMessage", text);
                lastMessageUpdate.put("time", sdf.format(new Date()));

                FirebaseDatabase.getInstance().getReference("chats").child(chatId).updateChildren(lastMessageUpdate);
            } else {
                Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCachedMessages() {
        if (chatId == null) return;
        ArrayList<MessageModel> cached = cacheManager.loadMessages(chatId);
        for (MessageModel msg : cached) {
            messagesList.add(msg);
            loadedTimestamps.add(msg.getTimestampLong());
        }
        messageAdapter.notifyDataSetChanged();
        if (!messagesList.isEmpty()) {
            messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
        }
    }

    private void saveToCache() {
        if (chatId != null) {
            cacheManager.saveMessages(chatId, messagesList);
        }
    }

    private ChildEventListener buildMessagesListener() {
        return new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String text = snapshot.child("text").getValue(String.class);
                String senderUsername = snapshot.child("senderUsername").getValue(String.class);
                String time = snapshot.child("time").getValue(String.class);
                Long timestampLong = snapshot.child("timestamp").getValue(Long.class);

                if (text != null && senderUsername != null && timestampLong != null) {
                    if (loadedTimestamps.contains(timestampLong)) {
                        return;
                    }
                    loadedTimestamps.add(timestampLong);
                    boolean sentByMe = senderUsername.equals(currentUsername);
                    messagesList.add(new MessageModel(text, senderUsername, time, timestampLong, sentByMe));
                    messageAdapter.notifyItemInserted(messagesList.size() - 1);
                    messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        profileEmail.setText(user.getEmail());

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                profileName.setText(snapshot.child("name").getValue(String.class));
                profileUsername.setText(snapshot.child("username").getValue(String.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (chatId != null) {
            ChatCacheApplication.setCurrentOpenChatId(chatId);
        }
        if (messagesRef != null && messagesListener != null) {
            messagesRef.addChildEventListener(messagesListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveToCache();
        ChatCacheApplication.setCurrentOpenChatId(null);
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }
}