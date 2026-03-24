package com.example.mainproj.messaging.friendRequest;

import android.os.Bundle;
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

public class IncomingRequestsActivity extends BaseActivity implements FriendRequestAdapter.OnRequestActionListener {

    private RecyclerView recyclerView;
    private FriendRequestAdapter adapter;
    private List<FriendRequest> requests = new ArrayList<>();
    private DatabaseReference requestsRef;
    private ValueEventListener requestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_incoming_requests);

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
        adapter = new FriendRequestAdapter(this);
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
        requestsRef = FirebaseDatabase.getInstance().getReference("friendRequests").child(safeEmail);

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

        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        String chatId = chatsRef.push().getKey();
        if (chatId == null) return;

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("username", request.getFromUsername());
        chatData.put("lastMessage", "");
        chatData.put("time", "");
        chatData.put("bio", "");
        chatData.put("lastSeen", "");
        chatData.put("online", false);

        chatsRef.child(chatId).setValue(chatData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                requestsRef.child(request.getKey()).removeValue().addOnCompleteListener(task2 -> {
                    Toast.makeText(IncomingRequestsActivity.this, "Chat created with " + request.getFromUsername(), Toast.LENGTH_SHORT).show();
                });
            } else {
                Toast.makeText(IncomingRequestsActivity.this, "Failed to create chat", Toast.LENGTH_SHORT).show();
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