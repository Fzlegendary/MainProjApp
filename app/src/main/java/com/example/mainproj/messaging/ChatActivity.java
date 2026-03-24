package com.example.mainproj.messaging;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ChatActivity extends BaseActivity {

    private static final String PREFS_NAME = "chat_prefs";
    private static final String CACHE_KEY  = "cached_chats";

    private DrawerLayout drawerLayout;
    private Toolbar chatToolbar;

    private ArrayList<ChatModel> chatList;
    private ChatAdapter chatAdapter;

    private DatabaseReference chatsRef;
    private ValueEventListener chatsListener;

    // ✅ NEW: Profile drawer views
    private ImageView profileImage;
    private TextView profileName, profileUsername, profileEmail;

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

        String partnerUsername = getIntent().getStringExtra("username");
        if (partnerUsername != null && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(partnerUsername);
        }

        // ✅ CHANGED: open RIGHT drawer instead of LEFT
        chatToolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(Gravity.END)
        );

        chatList    = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList, this);

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

        loadUserProfile(); // ✅ NEW

        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(ChatActivity.this, Login_activity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // ─── Existing logic (UNCHANGED) ───────────────────────

        loadFromCache();

        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        chatsListener = buildFirebaseListener();
    }

    // ✅ NEW FUNCTION: load user data into drawer
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

    private ValueEventListener buildFirebaseListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String username = child.child("username").getValue(String.class);
                    String lastMessage = child.child("lastMessage").getValue(String.class);
                    String time = child.child("time").getValue(String.class);
                    String bio = child.child("bio").getValue(String.class);
                    String lastSeen = child.child("lastSeen").getValue(String.class);
                    Boolean online = child.child("online").getValue(Boolean.class);

                    if (username != null) {
                        chatList.add(new ChatModel(
                                username,
                                lastMessage != null ? lastMessage : "",
                                time != null ? time : "",
                                bio != null ? bio : "",
                                lastSeen != null ? lastSeen : "",
                                online != null && online
                        ));
                    }
                }

                chatAdapter.notifyDataSetChanged();
                saveToCache(chatList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
    }

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
        } catch (Exception ignored) {}
    }

    private void saveToCache(ArrayList<ChatModel> list) {
        try {
            JSONArray array = new JSONArray();
            for (ChatModel chat : list) {
                JSONObject obj = new JSONObject();
                obj.put("username", chat.getUsername());
                obj.put("lastMessage", chat.getLastMessage());
                obj.put("time", chat.getTime());
                obj.put("bio", chat.getBio());
                obj.put("lastSeen", chat.getLastSeen());
                obj.put("online", chat.isOnline());
                array.put(obj);
            }
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(CACHE_KEY, array.toString())
                    .apply();
        } catch (Exception ignored) {}
    }
}