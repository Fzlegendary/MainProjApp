package com.example.mainproj.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
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
import com.example.mainproj.messaging.ChatCacheApplication;
import com.example.mainproj.messaging.ChatCacheManager;
import com.example.mainproj.messaging.ChatMetadataManager;
import com.example.mainproj.messaging.ChatModel;
import com.example.mainproj.messaging.GeminiManager;
import com.example.mainproj.messaging.MessagingActivity;
import com.example.mainproj.auth.SessionManager;
import com.example.mainproj.messaging.NetworkMonitor;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ChatsFragment extends Fragment {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private ArrayList<ChatModel> chatList = new ArrayList<>();
    private ArrayList<ChatModel> filteredChatList = new ArrayList<>();

    private EditText searchChats;
    private FloatingActionButton addChatButton;
    private View btnFriendRequests;
    private View btnFriends;

    private DatabaseReference chatsRef;
    private ChildEventListener chatsListener;
    private String currentUserSafeEmail;

    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isFirebaseListenerAttached = false;

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
        btnFriends = view.findViewById(R.id.btnFriends);
        setupRecyclerView();
        setupSearch();
        setupClickListeners();
        loadChats();
    }

    private void setupRecyclerView() {
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String safeEmail = user != null && user.getEmail() != null
                ? user.getEmail().replace(".", "_") : "";
        chatAdapter = new ChatAdapter(requireContext(), filteredChatList, safeEmail);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupSearch() {
        searchChats.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                chatAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterChats(String query) {
        chatAdapter.filter(query);
    }

    private String formatLastMessage(DataSnapshot snapshot) {
        String text = snapshot.child("lastMessage").child("text").getValue(String.class);
        String sender = snapshot.child("lastMessage").child("senderUsername").getValue(String.class);
        if (text == null) return "";

        String currentUsername = ChatCacheApplication.getCurrentUsername();
        boolean isMe = sender != null && sender.equals(currentUsername);
        return isMe ? "You" + ": " + text : text;
    }

    private void setupClickListeners() {
        addChatButton.setOnClickListener(v -> showAddUserDialog());
        btnFriendRequests.setOnClickListener(v -> showFriendRequestsDialog());
        btnFriends.setOnClickListener(v -> showFriendsDialog());

        View btnGemini = getView().findViewById(R.id.btnGemini);
        if (btnGemini != null) {
            btnGemini.setOnClickListener(v -> openGeminiChat());
        }
    }

    private void openGeminiChat() {
        String cachedUsername = ChatCacheApplication.getCurrentUsername();
        if (cachedUsername != null) {
            launchGeminiWithUsername(cachedUsername);
            return;
        }

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null || fbUser.getEmail() == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String safeEmail = fbUser.getEmail().replace(".", "_");
        FirebaseDatabase.getInstance().getReference("users").child(safeEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String username = snapshot.child("username").getValue(String.class);
                        if (username != null) {
                            ChatCacheApplication.setCurrentUsername(username);
                            launchGeminiWithUsername(username);
                        } else {
                            Toast.makeText(requireContext(), "Could not identify user", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Error fetching user", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void launchGeminiWithUsername(String username) {
        String chatId = GeminiManager.getInstance(requireContext()).buildChatId(username);

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null || fbUser.getEmail() == null) {
            startGeminiActivity(chatId);
            return;
        }
        String mySafeEmail = fbUser.getEmail().replace(".", "_");

        DatabaseReference chatRootRef = FirebaseDatabase.getInstance()
                .getReference("chats").child(chatId);
        chatRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("lastMessage", "");
                    chatData.put("timestamp", ServerValue.TIMESTAMP);
                    chatData.put("lastSeen", ServerValue.TIMESTAMP);
                    chatData.put("online", false);
                    chatData.put("createdAt", ServerValue.TIMESTAMP);

                    chatRootRef.setValue(chatData);

                    Map<String, Object> userChatEntry = new HashMap<>(chatData);
                    userChatEntry.put("username", "Gemini");
                    userChatEntry.put("nickname", "AI Assistant");
                    FirebaseDatabase.getInstance()
                            .getReference("users").child(mySafeEmail)
                            .child("chats").child(chatId)
                            .setValue(userChatEntry);
                }
                startGeminiActivity(chatId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                startGeminiActivity(chatId);
            }
        });
    }

    private void startGeminiActivity(String chatId) {
        Intent intent = new Intent(requireContext(), MessagingActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("username", "Gemini");
        startActivity(intent);
    }

    private void loadChats() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUserSafeEmail = user.getEmail().replace(".", "_");

        if (NetworkMonitor.getInstance(requireContext()).isConnected()) {
            attachFirebaseListener();
        } else {
            ArrayList<ChatModel> cached = ChatMetadataManager.getInstance(requireContext())
                    .loadAllChats(currentUserSafeEmail);
            if (!cached.isEmpty()) {
                chatList.clear();
                chatList.addAll(cached);
                chatAdapter.updateFullList(chatList);
                chatAdapter.filter("");
            }
            registerNetworkCallback();
        }
    }

    private void attachFirebaseListener() {
        if (isFirebaseListenerAttached) return;

        chatsRef = FirebaseDatabase.getInstance()
                .getReference("users").child(currentUserSafeEmail).child("chats");

        chatsListener = new ChildEventListener() {
            private boolean initialLoadDone = false;

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String chatId = snapshot.getKey();
                if (chatId == null) return;

                ChatModel chat = new ChatModel();
                chat.setChatId(chatId);
                chat.setUsername(snapshot.child("username").getValue(String.class));
                chat.setNickname(snapshot.child("nickname").getValue(String.class));
                chat.setImageUrl(snapshot.child("imageUrl").getValue(String.class));
                chat.setOnline(Boolean.TRUE.equals(snapshot.child("online").getValue(Boolean.class)));
                chat.setUnreadCount(snapshot.child("unreadCount").getValue(Long.class));
                chat.setLastMessage(formatLastMessage(snapshot));

                Long ts = snapshot.child("lastMessage").child("timestamp").getValue(Long.class);
                if (ts != null) chat.setTimestamp(ts);

                String chatName = snapshot.child("chatName").getValue(String.class);
                if (chatName != null && !chatName.isEmpty()) chat.setNickname(chatName);

                boolean exists = chatList.stream().anyMatch(c -> chatId.equals(c.getChatId()));
                if (!exists) {
                    chatList.add(chat);
                    if (currentUserSafeEmail != null) {
                        ChatMetadataManager.getInstance(requireContext()).saveChat(currentUserSafeEmail, chat);
                    }
                }

                chatAdapter.updateFullList(chatList);
                chatAdapter.filter(searchChats.getText().toString());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String chatId = snapshot.getKey();
                if (chatId == null) return;

                for (int i = 0; i < chatList.size(); i++) {
                    ChatModel existing = chatList.get(i);
                    if (chatId.equals(existing.getChatId())) {
                        ChatModel updated = new ChatModel();
                        updated.setChatId(chatId);
                        updated.setUsername(snapshot.child("username").getValue(String.class));
                        updated.setNickname(snapshot.child("nickname").getValue(String.class));
                        updated.setImageUrl(snapshot.child("imageUrl").getValue(String.class));
                        updated.setOnline(Boolean.TRUE.equals(snapshot.child("online").getValue(Boolean.class)));
                        updated.setUnreadCount(snapshot.child("unreadCount").getValue(Long.class));
                        updated.setLastMessage(formatLastMessage(snapshot));

                        Long ts = snapshot.child("lastMessage").child("timestamp").getValue(Long.class);
                        if (ts != null) updated.setTimestamp(ts);

                        String chatName = snapshot.child("chatName").getValue(String.class);
                        if (chatName != null && !chatName.isEmpty()) updated.setNickname(chatName);

                        chatList.set(i, updated);
                        if (currentUserSafeEmail != null) {
                            ChatMetadataManager.getInstance(requireContext()).saveChat(currentUserSafeEmail, updated);
                        }
                        chatAdapter.updateFullList(chatList);
                        chatAdapter.filter(searchChats.getText().toString());
                        break;
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String chatId = snapshot.getKey();
                chatList.removeIf(c -> c.getChatId() != null && c.getChatId().equals(chatId));
                if (chatId != null && currentUserSafeEmail != null) {
                    ChatMetadataManager.getInstance(requireContext()).clearChat(currentUserSafeEmail, chatId);
                    ChatCacheManager.getInstance(requireContext()).clearChat(chatId);
                }
                chatAdapter.updateFullList(chatList);
                chatAdapter.filter(searchChats.getText().toString());
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Error loading chats", Toast.LENGTH_SHORT).show();
            }
        };

        chatsRef.addChildEventListener(chatsListener);
        isFirebaseListenerAttached = true;
    }

    private void registerNetworkCallback() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> attachFirebaseListener());
                }
            }
        };
        NetworkMonitor.getInstance(requireContext()).registerNetworkCallback(networkCallback);
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
                                    FirebaseDatabase.getInstance()
                                            .getReference("users").child(safeCurrentEmail)
                                            .child("friends").child(targetUsername)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot friendSnap) {
                                                    if (friendSnap.exists()) {
                                                        Toast.makeText(requireContext(), "Friend already added", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        sendRequestToUser(targetEmail, safeCurrentEmail, myUsername, myImageUrl, targetUsername);
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    sendRequestToUser(targetEmail, safeCurrentEmail, myUsername, myImageUrl, targetUsername);
                                                }
                                            });
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
        SessionManager.sendFriendRequest(safeTargetEmail, targetUsername);

        if (fromImageUrl != null && !fromImageUrl.isEmpty()) {
            FirebaseDatabase.getInstance()
                    .getReference("friendRequests").child(safeTargetEmail)
                    .child(fromUsername).child("fromImageUrl")
                    .setValue(fromImageUrl);
        }

        Toast.makeText(requireContext(), "Friend request sent to " + targetUsername, Toast.LENGTH_SHORT).show();
    }

    private void showFriendRequestsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_friend_requests, null);
        androidx.viewpager2.widget.ViewPager2 viewPager = dialogView.findViewById(R.id.viewPager);
        com.google.android.material.tabs.TabLayout tabLayout = dialogView.findViewById(R.id.tabLayout);

        FriendRequestsPagerAdapter pagerAdapter = new FriendRequestsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new com.google.android.material.tabs.TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("Incoming");
            else tab.setText("Outgoing");
        }).attach();

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .create()
                .show();
    }

    private void showFriendsDialog() {
        FriendsListFragment friendsFragment = new FriendsListFragment();
        friendsFragment.show(getParentFragmentManager(), "FriendsList");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatsRef != null && chatsListener != null) {
            chatsRef.removeEventListener(chatsListener);
        }
        if (networkCallback != null) {
            NetworkMonitor.getInstance(requireContext()).unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
        isFirebaseListenerAttached = false;
    }
}