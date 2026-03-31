package com.example.mainproj.messaging.friendRequest;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.BaseActivity;
import com.example.mainproj.R;
import com.example.mainproj.messaging.ChatCacheApplication;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.mainproj.messaging.ChatUtils;
import com.google.firebase.database.ServerValue;

public class IncomingRequestsActivity extends BaseActivity implements FriendRequestAdapter.OnRequestActionListener {

    private RecyclerView recyclerView;
    private FriendRequestAdapter adapter;
    private List<FriendRequest> requests = new ArrayList<>();
    private DatabaseReference requestsRef;
    private ValueEventListener requestsListener;
    private final String TAG = "IRA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_incoming_requests);
        clearActiveNav();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendRequestAdapter(this, this);
        recyclerView.setAdapter(adapter);

        loadRequests();
    }

    private void loadRequests() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String safeEmail = user.getEmail().replace(".", "_");
        DatabaseReference dr = FirebaseDatabase.getInstance().getReference("users").child(safeEmail).child("username");
        Log.d(TAG, "loadRequests: safe email: " + safeEmail);
        Log.d(TAG, "Reference path: @" + dr.toString());

        dr.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String value = snapshot.getValue(String.class);
                Log.d(TAG, "onDataChange: value: " + value);
                requestsRef = FirebaseDatabase.getInstance().getReference("friend_request").child(value);
                Log.d(TAG, "onDataChange: value of current username: " + value);

                requestsListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        requests.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            if ("pending".equals(snap.child("status").getValue(String.class))) {
                                FriendRequest request = snap.getValue(FriendRequest.class);
                                if (request != null) {
                                    request.setKey(snap.getKey());
                                    requests.add(request);
                                }
                            }
                        }
                        adapter.setRequests(requests);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(IncomingRequestsActivity.this, "Error loading requests", Toast.LENGTH_SHORT).show();
                    }
                };

                requestsRef.addValueEventListener(requestsListener);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsRef != null && requestsListener != null) {
            requestsRef.removeEventListener(requestsListener);
        }
    }

    @Override
    public void onAccept(FriendRequest request) {
        if (request.getKey() == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) return;

        String myEmail = currentUser.getEmail().replace(".", "_");
        String fromEmail = request.getFrom().replace(".", "_");

        String myUsername = ChatCacheApplication.getCurrentUsername();
        if (myUsername == null) {
            Toast.makeText(this, "Unable to get your username", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.child(fromEmail).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String partnerUsername = snapshot.getValue(String.class);
                if (partnerUsername == null || partnerUsername.isEmpty()) {
                    Toast.makeText(IncomingRequestsActivity.this, "Unable to get partner username", Toast.LENGTH_SHORT).show();
                    return;
                }

                String chatId = ChatUtils.generateChatId(myUsername, partnerUsername);
                if (chatId == null) {
                    Toast.makeText(IncomingRequestsActivity.this, "Error generating chat ID", Toast.LENGTH_SHORT).show();
                    return;
                }

                usersRef.child(myEmail).child("nickname").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot myNickSnapshot) {
                        String myNickname = myNickSnapshot.getValue(String.class);

                        usersRef.child(fromEmail).child("nickname").addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot partnerNickSnapshot) {
                                String partnerNickname = partnerNickSnapshot.getValue(String.class);
                                checkAndCreateChat(chatId, myUsername, partnerUsername, myNickname, partnerNickname);

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(IncomingRequestsActivity.this, "Error fetching partner nickname", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(IncomingRequestsActivity.this, "Error fetching your nickname", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(IncomingRequestsActivity.this, "Error fetching partner username", Toast.LENGTH_SHORT).show();
            }
        });

        requestsRef.child(request.getKey()).removeValue();
    }

    private void checkAndCreateChat(String chatId, String myUsername,
                                    String partnerUsername, String myNickname, String partnerNickname) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        DatabaseReference specificChatRef = chatsRef.child(chatId);

        specificChatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(IncomingRequestsActivity.this, "Chat already exists with " + partnerUsername, Toast.LENGTH_SHORT).show();
                } else {
                    createNewChat(chatId, myUsername, partnerUsername, myNickname, partnerNickname);
                }
                specificChatRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(IncomingRequestsActivity.this, "Error checking for existing chat", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNewChat(String chatId, String myUsername,
                               String partnerUsername, String myNickname, String partnerNickname) {

        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("lastMessage", "Unknown");
        chatData.put("time", System.currentTimeMillis());
        chatData.put("online", true);
        chatData.put("createdAt", System.currentTimeMillis());

        Map<String, Object> participants = new HashMap<>();
        participants.put(myUsername, true);
        participants.put(partnerUsername, true);
        chatData.put("participants", participants);

        chatData.put("nickname_of_" + myUsername, partnerNickname != null ? partnerNickname : "");
        chatData.put("nickname_of_" + partnerUsername, myNickname != null ? myNickname : "");

        chatsRef.child(chatId).setValue(chatData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(IncomingRequestsActivity.this,
                        "Chat created with " + partnerUsername,
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(IncomingRequestsActivity.this,
                        "Failed to create chat",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onReject(FriendRequest request) {
        if (request.getKey() == null) return;
        requestsRef.child(request.getKey()).removeValue().addOnCompleteListener(task -> {
            Toast.makeText(IncomingRequestsActivity.this, "Request rejected", Toast.LENGTH_SHORT).show();
        });
    }
}