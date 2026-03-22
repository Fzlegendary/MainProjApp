package com.example.mainproj.messaging;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MessagingActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private ArrayList<ChatModel> chatList;

    private EditText searchChats;
    private FloatingActionButton addChatButton;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    // ─── Cache ────────────────────────────────────────────────────────────────
    private static final String PREFS_NAME = "chat_prefs";
    private static final String CACHE_KEY  = "cached_chats";

    // ─── Firebase ─────────────────────────────────────────────────────────────
    private DatabaseReference  chatsRef;
    private ValueEventListener chatsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_messaging);

        // initialize views
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        searchChats = findViewById(R.id.searchChats);
        addChatButton = findViewById(R.id.addChatButton);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationDrawer);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        // drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open,
                R.string.close
        );

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // chat list
        chatList = new ArrayList<>();

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatList, this);
        chatRecyclerView.setAdapter(chatAdapter);

        // Show cached chats immediately (instant UI), then overwrite from Firebase
        loadFromCache();

        // TODO: replace "chats" with your actual Firebase path, e.g. "users/" + uid + "/chats"
        chatsRef     = FirebaseDatabase.getInstance().getReference("chats");
        chatsListener = buildFirebaseListener();

        addChatButton.setOnClickListener(v -> {

            Toast.makeText(this,"Open Add User Screen",Toast.LENGTH_SHORT).show();

        });

        // drawer menu clicks
        navigationView.setNavigationItemSelectedListener(item -> {

            if(item.getItemId()==R.id.menu_logout){

                Toast.makeText(this,"Logging out",Toast.LENGTH_SHORT).show();

                finish();

            }

            return true;
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