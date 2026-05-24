package com.example.mainproj.messaging;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MessagesFragment extends Fragment {

    private DrawerLayout drawerLayout;
    private Toolbar chatToolbar;

    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private ArrayList<MessageModel> messagesList;
    private EditText messageInput;
    private MaterialButton sendButton;

    private String chatId;
    private String partnerUsername;
    private String currentUsername;
    private String currentUserUid;

    private DatabaseReference messagesRef;
    private ChildEventListener messagesListener;

    private ImageView profileImage;
    private TextView profileName, profileUsername, profileEmail;
    private GoogleSignInClient mGoogleSignInClient;
    private ChatCacheManager cacheManager;
    private ChatMetadataManager metadataManager;
    private Set<Long> loadedTimestamps = new HashSet<>();
    private ChatModel currentChat;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chatId = getArguments().getString("chatId");
            partnerUsername = getArguments().getString("username");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_conversation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        drawerLayout = view.findViewById(R.id.chatDrawerLayout);
        chatToolbar = view.findViewById(R.id.chatToolbar);

        if (requireActivity() instanceof BaseActivity) {
            ((BaseActivity) requireActivity()).setSupportActionBar(chatToolbar);
        }

        if (partnerUsername != null && chatToolbar != null) {
            chatToolbar.setTitle(partnerUsername);
        }

        chatToolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.END)
        );

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);

        messagesList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messagesList, requireContext());
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        messagesRecyclerView.setAdapter(messageAdapter);

        cacheManager = ChatCacheManager.getInstance(requireContext());
        metadataManager = ChatMetadataManager.getInstance(requireContext());
        loadCachedMessages();

        loadCurrentUsername();

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserUid = firebaseUser.getUid();
        }

        loadChatMetadata();

        if (chatId != null) {
            ChatCacheApplication.setCurrentOpenChatId(chatId);
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        profileImage = view.findViewById(R.id.profileImage);
        profileName = view.findViewById(R.id.profileName);
        profileUsername = view.findViewById(R.id.profileUsername);
        profileEmail = view.findViewById(R.id.profileEmail);

        loadUserProfile();

        view.findViewById(R.id.logoutButton).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            mGoogleSignInClient.signOut();

            android.content.Intent intent = new android.content.Intent(requireContext(), Login_activity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        sendButton.setOnClickListener(v -> sendMessage());

        if (chatId == null) {
            Toast.makeText(requireContext(), "Error: No chat ID", Toast.LENGTH_SHORT).show();
            return;
        }

        messagesRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages");
        messagesListener = buildMessagesListener();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (chatId != null) {
            ChatCacheApplication.setCurrentOpenChatId(chatId);
        }
        if (messagesRef != null && messagesListener != null) {
            messagesRef.addChildEventListener(messagesListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        saveToCache();
        ChatCacheApplication.setCurrentOpenChatId(null);
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }

    private void loadCurrentUsername() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

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

    private void loadChatMetadata() {
        if (chatId == null || currentUserUid == null) return;

        ArrayList<ChatModel> allChats = metadataManager.loadAllChats(currentUserUid);
        for (ChatModel chat : allChats) {
            if (chat == null || chat.getChatId() == null) continue;
            if (chatId.equals(chat.getChatId())) {
                currentChat = chat;

                if (chat.getUnreadCount() != null && chat.getUnreadCount() > 0) {
                    chat.setUnreadCount(0L);
                    metadataManager.saveChat(currentUserUid, chat);
                }
                return;
            }
        }
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
                Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
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
                    boolean sentByMe = currentUsername != null && senderUsername.equals(currentUsername);
                    messagesList.add(new MessageModel(text, senderUsername, time, timestampLong, sentByMe));

                    if (!sentByMe && currentUserUid != null && currentChat != null) {
                        Long currentCount = currentChat.getUnreadCount();
                        currentChat.setUnreadCount((currentCount != null ? currentCount : 0L) + 1L);
                        metadataManager.saveChat(currentUserUid, currentChat);
                    }

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
        if (user == null || user.getEmail() == null) return;

        profileEmail.setText(user.getEmail());

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getEmail().replace(".", "_"));

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
}