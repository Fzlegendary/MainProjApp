package com.example.mainproj.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.example.mainproj.messaging.ChatActivity;
import com.example.mainproj.messaging.ChatUtils;
import com.example.mainproj.messaging.friendRequest.FriendRequest;
import com.example.mainproj.messaging.friendRequest.FriendRequestAdapter;
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

public class IncomingRequestsFragment extends Fragment implements FriendRequestAdapter.OnRequestActionListener {

    private RecyclerView recyclerView;
    private String TAG = "fq";
    private FriendRequestAdapter adapter;
    private List<FriendRequest> requests = new ArrayList<>();
    private DatabaseReference requestsRef;
    private ValueEventListener requestsListener;
    private String requestKey;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_requests_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FriendRequestAdapter(requireContext(), this);
        recyclerView.setAdapter(adapter);

        loadRequests();
    }

    private void loadRequests() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), "Error loading requests", Toast.LENGTH_SHORT).show();
            }
        };
        requestsRef.addValueEventListener(requestsListener);
    }

    @Override
    public void onAccept(FriendRequest request) {
        if (request.getKey() == null) return;
        requestKey = request.getKey();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String myEmail = currentUser.getEmail().replace(".", "_");
        String fromEmail = request.getFrom().replace(".", "_");
        String partnerUsername = request.getFromUsername();

        FirebaseDatabase.getInstance().getReference("users").child(myEmail).child("username")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String myUsername = snapshot.getValue(String.class);
                        if (myUsername == null) {
                            Toast.makeText(requireContext(), "Unable to get your username", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String chatId = ChatUtils.generateChatId(myUsername, partnerUsername);
                        if (chatId == null) {
                            Toast.makeText(requireContext(), "Error generating chat ID", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        checkAndCreateChat(chatId, myEmail, fromEmail, myUsername, partnerUsername);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Error fetching your username", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndCreateChat(String chatId, String myEmail, String fromEmail, String myUsername, String partnerUsername) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        DatabaseReference specificChatRef = chatsRef.child(chatId);

        specificChatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(requireContext(), "Chat already exists with " + partnerUsername, Toast.LENGTH_SHORT).show();
                } else {
                    createNewChat(chatId, myEmail, fromEmail, myUsername, partnerUsername);
                }
                specificChatRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Error checking for existing chat", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNewChat(String chatId, String myEmail, String fromEmail, String myUsername, String partnerUsername) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.child(myEmail).child("nickname").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot myNickSnap) {
                String myNickname = myNickSnap.getValue(String.class);
                String myNick = myNickname != null ? myNickname : "";

                usersRef.child(fromEmail).child("nickname").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot partnerNickSnap) {
                        String partnerNickname = partnerNickSnap.getValue(String.class);
                        String partnerNick = partnerNickname != null ? partnerNickname : "";

                        Map<String, Object> chatData = new HashMap<>();
                        chatData.put("username", partnerUsername);
                        chatData.put("lastMessage", "");
                        chatData.put("time", "");
                        chatData.put("bio", "");
                        chatData.put("lastSeen", "");
                        chatData.put("online", false);
                        chatData.put("createdAt", com.google.firebase.database.ServerValue.TIMESTAMP);

                        Map<String, Object> participants = new HashMap<>();
                        participants.put(myEmail, true);
                        participants.put(fromEmail, true);
                        chatData.put("participants", participants);

                        chatsRef.child(chatId).setValue(chatData).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Map<String, Object> myChatData = new HashMap<>(chatData);
                                myChatData.put("username", partnerUsername);
                                myChatData.put("nickname", partnerNick);
                                myChatData.remove("participants");

                                Map<String, Object> partnerChatData = new HashMap<>(chatData);
                                partnerChatData.put("username", myUsername);
                                partnerChatData.put("nickname", myNick);
                                partnerChatData.remove("participants");

                                usersRef.child(myEmail).child("chats").child(chatId).setValue(myChatData);
                                usersRef.child(fromEmail).child("chats").child(chatId).setValue(partnerChatData);

                                requestsRef.child(requestKey).removeValue();

                                FirebaseDatabase.getInstance()
                                        .getReference("sentRequests").child(fromEmail).child(partnerUsername)
                                        .removeValue();
                                FirebaseDatabase.getInstance()
                                        .getReference("sentRequests").child(myEmail).child(partnerUsername)
                                        .removeValue();

                                Toast.makeText(requireContext(), "Chat created with " + partnerUsername, Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(requireContext(), ChatActivity.class);
                                intent.putExtra("username", partnerUsername);
                                intent.putExtra("chatId", chatId);
                                startActivity(intent);

                            } else {
                                Toast.makeText(requireContext(), "Failed to create chat", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Error fetching partner nickname", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Error fetching your nickname", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onReject(FriendRequest request) {
        if (request.getKey() == null) return;

        String senderEmail = request.getFrom();
        String senderSafe = senderEmail.replace(".", "_");
        String senderUsername = request.getFromUsername();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) return;
        String myEmail = currentUser.getEmail().replace(".", "_");

        FirebaseDatabase.getInstance().getReference("users").child(myEmail).child("username")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String myUsername = snapshot.getValue(String.class);
                        String myUsernameSafe = myUsername != null ? myUsername : "";

                        requestsRef.child(request.getKey()).removeValue();

                        FirebaseDatabase.getInstance()
                                .getReference("sentRequests").child(senderSafe).child(senderUsername)
                                .removeValue();
                        FirebaseDatabase.getInstance()
                                .getReference("sentRequests").child(myEmail).child(senderUsername)
                                .removeValue();

                        Toast.makeText(requireContext(), "Request rejected", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Error rejecting request", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestsRef != null && requestsListener != null) {
            requestsRef.removeEventListener(requestsListener);
        }
    }
}