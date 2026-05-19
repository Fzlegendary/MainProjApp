package com.example.mainproj.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.example.mainproj.messaging.ChatAdapter;
import com.example.mainproj.messaging.ChatMetadataManager;
import com.example.mainproj.messaging.ChatModel;
import com.example.mainproj.messaging.MessagesFragment;
import com.example.mainproj.messaging.friendRequest.FriendRequestAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatsFragment extends Fragment implements ChatAdapter.OnChatActionListener {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private ArrayList<ChatModel> chatList = new ArrayList<>();
    private ArrayList<ChatModel> filteredChatList = new ArrayList<>();

    private EditText searchChats;
    private FloatingActionButton addChatButton;
    private View btnFriendRequests;

    private DatabaseReference chatsRef;
    private ChildEventListener chatsListener;
    private String currentUserSafeEmail;
    private String currentUserUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        searchChats = view.findViewById(R.id.searchChats);
        addChatButton = view.findViewById(R.id.addChatButton);
        btnFriendRequests = view.findViewById(R.id.btnFriendRequests);

        setupRecyclerView();
        setupSearch();
        setupClickListeners();
        loadChats();
    }

    private void setupRecyclerView() {
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatAdapter = new ChatAdapter(filteredChatList, requireContext());
        chatAdapter.setOnChatActionListener(this);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupSearch() {
        searchChats.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterChats(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterChats(String query) {
        filteredChatList.clear();
        if (query.isEmpty()) {
            filteredChatList.addAll(chatList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (ChatModel chat : chatList) {
                String username = chat.getUsername() != null ? chat.getUsername().toLowerCase() : "";
                String nickname = chat.getNickname() != null ? chat.getNickname().toLowerCase() : "";
                if (username.contains(lowerQuery) || nickname.contains(lowerQuery)) {
                    filteredChatList.add(chat);
                }
            }
        }
        chatAdapter.notifyDataSetChanged();
    }

    private void setupClickListeners() {
        addChatButton.setOnClickListener(v -> showAddUserDialog());
        btnFriendRequests.setOnClickListener(v -> showFriendRequestsDialog());
    }

    private void loadChats() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUserUid = user.getUid();
        currentUserSafeEmail = user.getEmail().replace(".", "_");

        ArrayList<ChatModel> cachedChats = ChatMetadataManager.getInstance(requireContext())
                .loadAllChats(currentUserUid);
        chatList.clear();
        chatList.addAll(cachedChats);
        filterChats(searchChats.getText().toString());

        chatsRef = FirebaseDatabase.getInstance()
                .getReference("users").child(currentUserSafeEmail).child("chats");

        chatsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatModel chat = snapshot.getValue(ChatModel.class);
                if (chat != null) {
                    chat.setChatId(snapshot.getKey());

                    boolean chatAlreadyExists = false;
                    for (int index = 0; index < chatList.size(); index++) {
                        ChatModel existingChat = chatList.get(index);
                        if (existingChat != null && existingChat.getChatId() != null
                                && existingChat.getChatId().equals(chat.getChatId())) {
                            chatAlreadyExists = true;
                            break;
                        }
                    }

                    if (!chatAlreadyExists) {
                        chatList.add(chat);
                    }

                    ChatMetadataManager.getInstance(requireContext())
                            .saveChat(currentUserUid, chat);

                    filterChats(searchChats.getText().toString());
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatModel chat = snapshot.getValue(ChatModel.class);
                if (chat != null) {
                    String chatId = snapshot.getKey();

                    boolean chatFound = false;
                    for (int index = 0; index < chatList.size(); index++) {
                        ChatModel existingChat = chatList.get(index);
                        if (existingChat != null && existingChat.getChatId() != null
                                && existingChat.getChatId().equals(chatId)) {
                            chat.setChatId(chatId);
                            chatList.set(index, chat);

                            chatFound = true;
                            break;
                        }
                    }

                    ChatMetadataManager.getInstance(requireContext())
                            .saveChat(currentUserUid, chat);

                    if (chatFound)
                        filterChats(searchChats.getText().toString());
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String chatId = snapshot.getKey();

                chatList.removeIf(c -> c.getChatId() != null && c.getChatId().equals(chatId));

                for (int index = filteredChatList.size() - 1; index >= 0; index--) {
                    ChatModel chatItem = filteredChatList.get(index);
                    if (chatItem != null && chatItem.getChatId() != null
                            && chatItem.getChatId().equals(chatId)) {
                        filteredChatList.remove(index);
                    }
                }

                ChatMetadataManager.getInstance(requireContext())
                        .clearChat(currentUserUid, chatId);

                chatAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Error loading chats", Toast.LENGTH_SHORT).show();
            }
        };
        chatsRef.addChildEventListener(chatsListener);
    }

    private void showAddUserDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_user, null);
        EditText etUsername = dialogView.findViewById(R.id.etUsername);

        new AlertDialog.Builder(requireContext())
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
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentEmail = currentUser.getEmail();
        String safeCurrentEmail = currentEmail.replace(".", "_");

        FirebaseDatabase.getInstance()
                .getReference("users").child(safeCurrentEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String myUsername = snapshot.child("username").getValue(String.class);
                        String myImageUrl = snapshot.child("imageUrl").getValue(String.class);
                        if (myUsername != null && myUsername.equalsIgnoreCase(targetUsername)) {
                            Toast.makeText(requireContext(), "You can't send a friend request to yourself", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        proceedWithFriendRequest(targetUsername, safeCurrentEmail, myUsername, myImageUrl);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        proceedWithFriendRequest(targetUsername, safeCurrentEmail, null, null);
                    }
                });
    }

    private void proceedWithFriendRequest(String targetUsername, String safeCurrentEmail, String myUsername, String myImageUrl) {
        FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("username").equalTo(targetUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnap : snapshot.getChildren()) {
                                String targetEmail = userSnap.child("email").getValue(String.class);
                                if (targetEmail != null) {
                                    sendRequestToUser(targetEmail, safeCurrentEmail, myUsername, myImageUrl, targetUsername);
                                    return;
                                }
                            }
                        }
                        Toast.makeText(requireContext(), "User not found: " + targetUsername, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Error finding user", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendRequestToUser(String targetEmail, String fromEmail, String fromUsername, String fromImageUrl, String targetUsername) {
        String safeTargetEmail = targetEmail.replace(".", "_");
        DatabaseReference requestsRef = FirebaseDatabase.getInstance()
                .getReference("friendRequests").child(safeTargetEmail);

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("from", fromEmail);
        requestData.put("fromUsername", fromUsername != null ? fromUsername : "Unknown");
        requestData.put("fromImageUrl", fromImageUrl != null ? fromImageUrl : "");
        requestData.put("timestamp", System.currentTimeMillis());
        requestData.put("status", "pending");

        requestsRef.child(fromUsername).setValue(requestData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference sentRef = FirebaseDatabase.getInstance()
                        .getReference("sentRequests").child(fromEmail);
                Map<String, Object> sentData = new HashMap<>();
                sentData.put("to", targetEmail.replace(".", "_"));
                sentData.put("toUsername", targetUsername);
                sentData.put("timestamp", System.currentTimeMillis());
                sentData.put("status", "pending");
                sentRef.child(targetUsername).setValue(sentData);

                Toast.makeText(requireContext(), "Friend request sent to " + targetUsername, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to send request", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFriendRequestsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_friend_requests, null);
        androidx.viewpager2.widget.ViewPager2 viewPager = dialogView.findViewById(R.id.viewPager);
        com.google.android.material.tabs.TabLayout tabLayout = dialogView.findViewById(R.id.tabLayout);

        FriendRequestsPagerAdapter pagerAdapter = new FriendRequestsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new com.google.android.material.tabs.TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Incoming");
            } else {
                tab.setText("Outgoing");
            }
        }).attach();

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .create();

        dialog.show();
    }

    @Override
    public void onOpenProfile(ChatModel chat) {
        Intent intent = new Intent(requireContext(), com.example.mainproj.ProfileDisplayerActivity.class);
        intent.putExtra("username", chat.getUsername());
        startActivity(intent);
    }

    @Override
    public void onCloseDM(ChatModel chat) {
        Toast.makeText(requireContext(), "Close DM: " + chat.getUsername(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChatClick(ChatModel chat) {
        Bundle args = new Bundle();
        args.putString("chatId", chat.getChatId());
        args.putString("username", chat.getUsername());

        MessagesFragment conversationFragment = new MessagesFragment();
        conversationFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.viewPager, conversationFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatsRef != null && chatsListener != null) {
            chatsRef.removeEventListener(chatsListener);
        }
    }
}