package com.example.mainproj.messaging;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.example.mainproj.messaging.friendRequest.IncomingRequestsActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class MessagingActivity extends BaseActivity implements ChatAdapter.OnChatActionListener {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private ArrayList<ChatModel> chatList;

    private FloatingActionButton addChatButton;
    private ImageButton btnFriendRequests;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private final String TAG = "msg";

    private static final String PREFS_NAME = "chat_prefs";
    private static final String CACHE_KEY  = "cached_chats";

    private DatabaseReference chatsRef;
    private String tempCurrentUsername;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_messaging);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        addChatButton = findViewById(R.id.addChatButton);
        btnFriendRequests = findViewById(R.id.btnFriendRequests);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationDrawer);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        clearActiveNav();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open, R.string.close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setupNavigationHeader();


        chatList = new ArrayList<>();

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatList, this);
        chatAdapter.setOnChatActionListener(this);
        chatRecyclerView.setAdapter(chatAdapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            loadCurrentUsername();
        }

        chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        addChatButton.setOnClickListener(v -> showAddUserDialog());
        btnFriendRequests.setOnClickListener(v ->
                startActivity(new Intent(this, IncomingRequestsActivity.class))
        );

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private void setupNavigationHeader() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        View headerView = navigationView.getHeaderView(0);
        ImageView headerImage = headerView.findViewById(R.id.profileImage);
        TextView headerName = headerView.findViewById(R.id.profileName);
        TextView headerEmail = headerView.findViewById(R.id.profileEmail);

        String email = user.getEmail();
        String safeEmail = email != null ? email.replace(".", "_") : null;

        if (email != null) {
            headerEmail.setText(email);
        }

        if (safeEmail != null) {
            FirebaseDatabase.getInstance().getReference("users")
                    .child(safeEmail)
                    .child("username")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String username = snapshot.getValue(String.class);
                            if (username != null && !username.isEmpty()) {
                                headerName.setText(username);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }

        Uri photoUri = user.getPhotoUrl();
        if (photoUri != null) {
            com.bumptech.glide.Glide.with(this)
                    .load(photoUri)
                    .circleCrop()
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .into(headerImage);
        }
    }

    private void loadCurrentUsername() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String safeEmail = user.getEmail().replace(".", "_");
        FirebaseDatabase.getInstance().getReference("users")
                .child(safeEmail)
                .child("username")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        tempCurrentUsername = snapshot.getValue(String.class);
                        if (tempCurrentUsername != null) {
                            ChatCacheApplication.setCurrentUsername(tempCurrentUsername);
                            Log.d(TAG, "loadCurrentUsername: tempUsername: " + tempCurrentUsername);
                        }
                        Log.d(TAG, "onDataChange: load chats:");
                        loadChats(tempCurrentUsername);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ✅ LOAD CHATS ASYNC
    private void loadChats(String username) {
        Log.d(TAG, "loadChats: currentusername: " + username);
        if (username == null) return;

        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange: in onDataChange");
                
                Map<String, ChatModel> updatedChats = new HashMap<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String chatId = child.getKey();
                    Log.d(TAG, "onDataChange: chatId: " + chatId);
                    if (chatId == null) continue;

                    DataSnapshot participantsSnapshot = child.child("participants");
                    if (participantsSnapshot == null || !participantsSnapshot.hasChild(username)) continue;

                    Log.d(TAG, "onDataChange: has child: true");

                    String partnerUsername = null;
                    for (DataSnapshot participant : participantsSnapshot.getChildren()) {
                        String key = participant.getKey();
                        if (key != null && !key.equals(username)) {
                            partnerUsername = key;
                            break;
                        }
                    }

                    String nickname = child.child("nickname_of_" + username).getValue(String.class);
                    String lastMessage = child.child("lastMessage").getValue(String.class);
                    Long timeLong = child.child("time").getValue(Long.class);
                    String bio = child.child("bio").getValue(String.class);
                    String lastSeen = child.child("lastSeen").getValue(String.class);
                    Boolean online = child.child("online").getValue(Boolean.class);

                    String time = "null";
                    if (timeLong != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                        time = sdf.format(new Date(timeLong));
                    }

                    Log.d(TAG, "onDataChange: chatid: " + chatId);
                    
                    int unreadCount = 0;
                    String currentOpenChatId = ChatCacheApplication.getCurrentOpenChatId();
                    if (chatId != null && chatId.equals(currentOpenChatId)) {
                        unreadCount = 0;
                    } else {
                        DataSnapshot unreadSnapshot = child.child("unread").child(username);
                        if (unreadSnapshot != null) {
                            Integer unread = unreadSnapshot.getValue(Integer.class);
                            unreadCount = unread != null ? unread : 0;
                        }
                    }

                    ChatModel chat = new ChatModel(
                            chatId,
                            partnerUsername != null ? partnerUsername : "null",
                            nickname != null ? nickname : "null",
                            lastMessage != null ? lastMessage : "null",
                            time,
                            bio != null ? bio : "null",
                            lastSeen != null ? lastSeen : "null",
                            online != null && online,
                            unreadCount
                    );
                    updatedChats.put(chatId, chat);
                    Log.d(TAG, "onDataChange: templist: " + chat.toString());
                }

                updateChatList(updatedChats);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateChatList(Map<String, ChatModel> updatedChats) {
        Set<String> newChatIds = updatedChats.keySet();
        
        for (int i = chatList.size() - 1; i >= 0; i--) {
            ChatModel existingChat = chatList.get(i);
            String chatId = existingChat.getChatId();
            
            if (!newChatIds.contains(chatId)) {
                chatList.remove(i);
                chatAdapter.notifyItemRemoved(i);
            }
        }
        
        for (int i = 0; i < chatList.size(); i++) {
            ChatModel existingChat = chatList.get(i);
            String chatId = existingChat.getChatId();
            ChatModel updatedChat = updatedChats.get(chatId);
            
            if (updatedChat != null) {
                boolean changed = false;
                
                if (!Objects.equals(existingChat.getLastMessage(), updatedChat.getLastMessage())) {
                    existingChat.setLastMessage(updatedChat.getLastMessage());
                    changed = true;
                }
                if (!Objects.equals(existingChat.getTime(), updatedChat.getTime())) {
                    existingChat.setTime(updatedChat.getTime());
                    changed = true;
                }
                if (existingChat.getUnreadCount() != updatedChat.getUnreadCount()) {
                    existingChat.setUnreadCount(updatedChat.getUnreadCount());
                    changed = true;
                }
                if (existingChat.isOnline() != updatedChat.isOnline()) {
                    existingChat.setOnline(updatedChat.isOnline());
                    changed = true;
                }
                
                if (changed) {
                    chatAdapter.notifyItemChanged(i);
                }
            }
        }
        
        for (String chatId : updatedChats.keySet()) {
            boolean found = false;
            for (ChatModel chat : chatList) {
                if (chat.getChatId().equals(chatId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                chatList.add(updatedChats.get(chatId));
                chatAdapter.notifyItemInserted(chatList.size() - 1);
            }
        }
        
        Collections.sort(chatList, (a, b) -> {
            long timeA = parseTimeToTimestamp(a.getTime());
            long timeB = parseTimeToTimestamp(b.getTime());
            return Long.compare(timeB, timeA);
        });
        chatAdapter.notifyDataSetChanged();
    }

    private long parseTimeToTimestamp(String timeStr) {
        if (timeStr == null || timeStr.equals("null")) return 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            return sdf.parse(timeStr).getTime();
        } catch (Exception e) {
            return 0;
        }
    }


    // ✅ CACHE
    private void loadFromCache() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(CACHE_KEY, null);
        if (json == null) return;

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                chatList.add(new ChatModel(
                        obj.optString("chatId"),
                        obj.optString("username"),
                        obj.optString("nickname"),
                        obj.optString("lastMessage"),
                        obj.optString("time"),
                        obj.optString("bio"),
                        obj.optString("lastSeen"),
                        obj.optBoolean("online", false)
                ));
            }
        } catch (Exception ignored) {}
    }

    private void saveToCache(ArrayList<ChatModel> list) {
        try {
            JSONArray array = new JSONArray();
            for (ChatModel chat : list) {
                JSONObject obj = new JSONObject();
                obj.put("chatId", chat.getChatId());
                obj.put("username", chat.getUsername());
                obj.put("nickname", chat.getNickname());
                obj.put("lastMessage", chat.getLastMessage());
                obj.put("time", chat.getTime());
                obj.put("bio", chat.getBio());
                obj.put("lastSeen", chat.getLastSeen());
                obj.put("online", chat.isOnline());
                array.put(obj);
            }

            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putString(CACHE_KEY, array.toString()).apply();

        } catch (Exception ignored) {}
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
                        DatabaseReference friendReq = FirebaseDatabase.getInstance().getReference("friend_request");
                        String safeEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".","_");
                        DatabaseReference dr = FirebaseDatabase.getInstance().getReference("users").child(safeEmail).child("username");
                        dr.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String value = snapshot.getValue(String.class);
                                HashMap<String, Object> requestData = new HashMap<>();
                                requestData.put("from", safeEmail);
                                requestData.put("fromUsername", value);
                                requestData.put("timestamp", System.currentTimeMillis());
                                requestData.put("status", "pending");
                                friendReq.child(username).child(value).setValue(requestData);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });

                        Toast.makeText(this, "sent request to " + username, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onOpenProfile(ChatModel chat) {}
    @Override
    public void onCloseDM(ChatModel chat) {}
}