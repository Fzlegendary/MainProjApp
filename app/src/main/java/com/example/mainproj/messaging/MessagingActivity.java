package com.example.mainproj.messaging;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.BaseActivity;
import com.example.mainproj.R;
import com.example.mainproj.messaging.friendRequest.FriendRequest;
import com.example.mainproj.messaging.friendRequest.IncomingRequestsActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MessagingActivity extends BaseActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private ArrayList<ChatModel> chatList;

    private EditText searchChats;
    private FloatingActionButton addChatButton;
    private ImageButton btnFriendRequests;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private static final String PREFS_NAME = "chat_prefs";
    private static final String CACHE_KEY  = "cached_chats";

    private DatabaseReference  chatsRef;
    private ValueEventListener chatsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_messaging);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        searchChats = findViewById(R.id.searchChats);
        addChatButton = findViewById(R.id.addChatButton);
        btnFriendRequests = findViewById(R.id.btnFriendRequests);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationDrawer);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open,
                R.string.close
        );

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        chatList = new ArrayList<>();

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatList, this);
        chatRecyclerView.setAdapter(chatAdapter);

        loadFromCache();

        chatsRef     = FirebaseDatabase.getInstance().getReference("chats");
        chatsListener = buildFirebaseListener();

        addChatButton.setOnClickListener(v -> showAddUserDialog());

        btnFriendRequests.setOnClickListener(v ->
                startActivity(new Intent(this, IncomingRequestsActivity.class)));

        navigationView.setNavigationItemSelectedListener(item -> {
            if(item.getItemId()==R.id.menu_logout){
                Toast.makeText(this,"Logging out",Toast.LENGTH_SHORT).show();
                finish();
            }
            return true;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private void showAddUserDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_user, null);
        EditText etUsername = dialogView.findViewById(R.id.etUsername);

        new AlertDialog.Builder(this)
                .setTitle("Add Friend")
                .setView(dialogView)
                .setPositiveButton("Send Request", (dialog, which) -> {
                    String username = etUsername.getText().toString().trim();
                    if (!username.isEmpty()) {
                        sendFriendRequest(username);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendFriendRequest(String targetUsername) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentEmail = currentUser.getEmail();
        String safeCurrentEmail = currentEmail != null ? currentEmail.replace(".", "_") : "";

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        
        usersRef.orderByChild("username").equalTo(targetUsername).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        String targetEmail = userSnap.child("email").getValue(String.class);
                        if (targetEmail != null) {
                            sendRequestToUser(targetEmail, safeCurrentEmail, targetUsername);
                            return;
                        }
                    }
                }
                Toast.makeText(MessagingActivity.this, "User not found: " + targetUsername, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MessagingActivity.this, "Error finding user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendRequestToUser(String targetEmail, String fromEmail, String fromUsername) {
        String safeTargetEmail = targetEmail.replace(".", "_");
        DatabaseReference requestsRef = FirebaseDatabase.getInstance()
                .getReference("friendRequests")
                .child(safeTargetEmail);

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("from", fromEmail);
        requestData.put("fromUsername", fromUsername);
        requestData.put("timestamp", System.currentTimeMillis());
        requestData.put("status", "pending");

        requestsRef.push().setValue(requestData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(MessagingActivity.this, "Friend request sent to " + fromUsername, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MessagingActivity.this, "Failed to send request", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Firebase lifecycle ────────────────────────────────────────────────────

    @Override
    protected void onStart() {
        super.onStart();
        if (chatsRef != null && chatsListener != null) {
            chatsRef.addValueEventListener(chatsListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (chatsRef != null && chatsListener != null) {
            chatsRef.removeEventListener(chatsListener);
        }
    }

    // ── Firebase listener ─────────────────────────────────────────────────────

    private ValueEventListener buildFirebaseListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String  username    = child.child("username").getValue(String.class);
                    String  lastMessage = child.child("lastMessage").getValue(String.class);
                    String  time        = child.child("time").getValue(String.class);
                    String  bio         = child.child("bio").getValue(String.class);
                    String  lastSeen    = child.child("lastSeen").getValue(String.class);
                    Boolean online      = child.child("online").getValue(Boolean.class);

                    if (username != null) {
                        chatList.add(new ChatModel(
                                username,
                                lastMessage != null ? lastMessage : "",
                                time        != null ? time        : "",
                                bio         != null ? bio         : "",
                                lastSeen    != null ? lastSeen    : "",
                                online      != null && online
                        ));
                    }
                }
                chatAdapter.notifyDataSetChanged();
                saveToCache(chatList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // cache is already shown — silently ignore
            }
        };
    }

    // ── Cache helpers ─────────────────────────────────────────────────────────

    private void loadFromCache() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(CACHE_KEY, null);
        if (json == null) return;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                chatList.add(new ChatModel(
                        obj.optString("username"),
                        obj.optString("lastMessage"),
                        obj.optString("time"),
                        obj.optString("bio"),
                        obj.optString("lastSeen"),
                        obj.optBoolean("online", false)
                ));
            }
            chatAdapter.notifyDataSetChanged();
        } catch (Exception ignored) { }
    }

    private void saveToCache(ArrayList<ChatModel> list) {
        try {
            JSONArray array = new JSONArray();
            for (ChatModel chat : list) {
                JSONObject obj = new JSONObject();
                obj.put("username",    chat.getUsername());
                obj.put("lastMessage", chat.getLastMessage());
                obj.put("time",        chat.getTime());
                obj.put("bio",         chat.getBio());
                obj.put("lastSeen",    chat.getLastSeen());
                obj.put("online",      chat.isOnline());
                array.put(obj);
            }
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putString(CACHE_KEY, array.toString()).apply();
        } catch (Exception ignored) { }
    }

    public void showChatOptions(ChatModel chat){

        String[] options={"Open Profile","Close DM"};

        AlertDialog.Builder builder=new AlertDialog.Builder(this);

        builder.setTitle(chat.getUsername());

        builder.setItems(options,(dialog,which)->{

            if(which==0){

                Toast.makeText(this,"Open profile",Toast.LENGTH_SHORT).show();

            }else if(which==1){

                Toast.makeText(this,"Close DM",Toast.LENGTH_SHORT).show();

            }

        });

        builder.show();
    }
}