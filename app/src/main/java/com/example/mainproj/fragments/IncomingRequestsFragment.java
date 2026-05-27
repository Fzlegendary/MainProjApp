package com.example.mainproj.fragments;

import android.os.Bundle;
import android.util.Log;
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
    private FriendRequestAdapter adapter;
    private List<FriendRequest> requests = new ArrayList<>();
    private DatabaseReference requestsRef;
    private ValueEventListener requestsListener;

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

        String from = request.getFrom();
        if (from == null || from.isEmpty()) return;
        String fromEmail = from.replace(".", "_");

        String partnerUsername = request.getFromUsername();
        if (partnerUsername == null || partnerUsername.isEmpty()) return;

        String partnerImageUrl = request.getFromImageUrl();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String myEmail = currentUser.getEmail().replace(".", "_");
        final String requestKey = request.getKey();

        FirebaseDatabase.getInstance().getReference("users").child(myEmail).child("username")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String myUsername = snapshot.getValue(String.class);
                        if (myUsername == null || myUsername.isEmpty()) {
                            Toast.makeText(requireContext(), "Unable to get your username", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String chatId = ChatUtils.generateChatId(myUsername, partnerUsername);
                        if (chatId == null) {
                            Toast.makeText(requireContext(), "Error generating chat ID", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        checkAndCreateChat(chatId, myEmail, fromEmail, myUsername, partnerUsername, requestKey, partnerImageUrl);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Error fetching your username", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndCreateChat(String chatId, String myEmail, String fromEmail, String myUsername, String partnerUsername, String requestKey, String partnerImageUrl) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        DatabaseReference specificChatRef = chatsRef.child(chatId);

        specificChatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(requireContext(), "Chat already exists with " + partnerUsername, Toast.LENGTH_SHORT).show();
                } else {
                    createNewChat(chatId, myEmail, fromEmail, myUsername, partnerUsername, requestKey, partnerImageUrl);
                }
                specificChatRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Error checking for existing chat", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNewChat(String chatId, String myEmail, String fromEmail, String myUsername, String partnerUsername, String requestKey, String partnerImageUrl) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.child(myEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot mySnap) {
                String myNickname = mySnap.child("nickname").getValue(String.class);
                String myNick = myNickname != null ? myNickname : "";
                String myImg = mySnap.child("imageUrl").getValue(String.class);
                if (myImg == null) myImg = "";

                final String finalMyImg = myImg;
                usersRef.child(fromEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot partnerSnap) {
                        String partnerNickname = partnerSnap.child("nickname").getValue(String.class);
                        String partnerNick = partnerNickname != null ? partnerNickname : "";
                        String partnerImg = partnerImageUrl != null ? partnerImageUrl : "";
                        if (partnerImg.isEmpty()) {
                            partnerImg = partnerSnap.child("imageUrl").getValue(String.class);
                            if (partnerImg == null) partnerImg = "";
                        }

                        Map<String, Object> chatData = new HashMap<>();
                        chatData.put("lastMessage", "");
                        chatData.put("timestamp", com.google.firebase.database.ServerValue.TIMESTAMP);
                        chatData.put("lastSeen", com.google.firebase.database.ServerValue.TIMESTAMP);
                        chatData.put("online", false);
                        chatData.put("createdAt", com.google.firebase.database.ServerValue.TIMESTAMP);

                        Map<String, Object> participants = new HashMap<>();
                        participants.put(myEmail, true);
                        participants.put(fromEmail, true);
                        chatData.put("participants", participants);

                        final String finalPartnerImg = partnerImg;
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

                                Map<String, Object> myFriendEntry = new HashMap<>();
                                myFriendEntry.put("safeEmail", fromEmail);
                                myFriendEntry.put("nickname", partnerNick);
                                myFriendEntry.put("imageUrl", finalPartnerImg);
                                myFriendEntry.put("chatId", chatId);
                                myFriendEntry.put("addedAt", System.currentTimeMillis());

                                Map<String, Object> partnerFriendEntry = new HashMap<>();
                                partnerFriendEntry.put("safeEmail", myEmail);
                                partnerFriendEntry.put("nickname", myNick);
                                partnerFriendEntry.put("imageUrl", finalMyImg);
                                partnerFriendEntry.put("chatId", chatId);
                                partnerFriendEntry.put("addedAt", System.currentTimeMillis());

                                usersRef.child(myEmail).child("friends").child(partnerUsername).setValue(myFriendEntry);
                                usersRef.child(fromEmail).child("friends").child(myUsername).setValue(partnerFriendEntry);

                                requestsRef.child(requestKey).removeValue();

                                FirebaseDatabase.getInstance()
                                        .getReference("sentRequests").child(fromEmail).child(myUsername)
                                        .removeValue();
                                FirebaseDatabase.getInstance()
                                        .getReference("sentRequests").child(myEmail).child(partnerUsername)
                                        .removeValue();

                                Toast.makeText(requireContext(), "Chat created with " + partnerUsername, Toast.LENGTH_SHORT).show();
                                Log.d("test", "chatid: " + chatId);

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
                Toast.makeText(requireContext(), "Error fetching your data", Toast.LENGTH_SHORT).show();
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
                    public void onDataChange(@NonNull DataSnapshot snapshot){
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