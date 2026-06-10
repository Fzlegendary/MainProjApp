package com.example.mainproj.messaging;

import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.example.mainproj.auth.SessionManager;
import com.example.mainproj.userSystem.Login_activity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;



public class MessagesFragment extends Fragment implements MessageAdapter.OnMessageDeleteListener {

    private DrawerLayout drawerLayout;

    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private ArrayList<MessageModel> messagesList;
    private EditText messageInput;
    private FloatingActionButton sendButton;

    private String chatId;
    private String partnerUsername;
    private String currentUsername;
    private String currentUserSafeEmail;

    private DatabaseReference messagesRef;
    private ValueEventListener messagesListener;
    private ConnectivityManager.NetworkCallback networkRecoveryCallback;

    private ImageView profileImage;
    private TextView profileName, profileUsername, profileEmail;

    private TextView tvChatName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chatId          = getArguments().getString("chatId");
            partnerUsername = getArguments().getString("username");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        drawerLayout = view.findViewById(R.id.chatDrawerLayout);

        tvChatName = view.findViewById(R.id.tvChatName);
        if (partnerUsername != null) {
            tvChatName.setText(partnerUsername);
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().onBackPressed());

        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView);
        messageInput         = view.findViewById(R.id.messageInput);
        sendButton           = view.findViewById(R.id.sendButton);

        messagesList    = new ArrayList<>();
        messageAdapter  = new MessageAdapter(messagesList, requireContext());
        messageAdapter.setOnMessageDeleteListener(this);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        messagesRecyclerView.setAdapter(messageAdapter);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && firebaseUser.getEmail() != null) {
            currentUserSafeEmail = firebaseUser.getEmail().replace(".", "_");
        }

        profileImage    = view.findViewById(R.id.profileImage);
        profileName     = view.findViewById(R.id.profileName);
        profileUsername = view.findViewById(R.id.profileUsername);
        profileEmail    = view.findViewById(R.id.profileEmail);

        loadUserProfile();

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            drawerLayout.closeDrawer(GravityCompat.START);
                        } else {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });

        view.findViewById(R.id.logoutButton).setOnClickListener(v -> {
            SessionManager.clearAllPrefs(requireContext());
            FirebaseAuth.getInstance().signOut();
            android.content.Intent intent = new android.content.Intent(
                    requireContext(), Login_activity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        view.findViewById(R.id.clearChatsButton).setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Delete Chat")
                        .setMessage("Are you sure you want to delete this chat?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (!NetworkMonitor.getInstance(requireContext()).isConnected()) {
                                if (requireContext().getSharedPreferences("offline_snooze_prefs", Context.MODE_PRIVATE)
                                        .getLong("snoozed_until", 0) > System.currentTimeMillis()) {
                                    ChatCacheManager.getInstance(requireContext()).clearChat(chatId);
                                    if (currentUserSafeEmail != null) {
                                        ChatMetadataManager.getInstance(requireContext()).clearChat(currentUserSafeEmail, chatId);
                                    }
                                    Toast.makeText(requireContext(), "Chat deleted", Toast.LENGTH_SHORT).show();
                                    requireActivity().onBackPressed();
                                    return;
                                }
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("No Internet Connection")
                                        .setMessage("No internet connection, action will not be synced out of device.")
                                        .setPositiveButton("OK", (d, w) -> {
                                            ChatCacheManager.getInstance(requireContext()).clearChat(chatId);
                                            if (currentUserSafeEmail != null) {
                                                ChatMetadataManager.getInstance(requireContext()).clearChat(currentUserSafeEmail, chatId);
                                            }
                                            Toast.makeText(requireContext(), "Chat deleted", Toast.LENGTH_SHORT).show();
                                            requireActivity().onBackPressed();
                                        })
                                        .setNegativeButton("Don't show for 30 min", (d, w) -> {
                                            requireContext().getSharedPreferences("offline_snooze_prefs", Context.MODE_PRIVATE)
                                                    .edit().putLong("snoozed_until", System.currentTimeMillis() + 1800000).apply();
                                            ChatCacheManager.getInstance(requireContext()).clearChat(chatId);
                                            if (currentUserSafeEmail != null) {
                                                ChatMetadataManager.getInstance(requireContext()).clearChat(currentUserSafeEmail, chatId);
                                            }
                                            Toast.makeText(requireContext(), "Chat deleted", Toast.LENGTH_SHORT).show();
                                            requireActivity().onBackPressed();
                                        })
                                        .setNeutralButton("Cancel", null)
                                        .show();
                                return;
                            }
                            ChatCacheManager.getInstance(requireContext()).clearAll();
                            if (chatId != null && currentUserSafeEmail != null) {
                                FirebaseDatabase.getInstance().getReference("users")
                                        .child(currentUserSafeEmail).child("chats").child(chatId)
                                        .removeValue();
                            }
                            Toast.makeText(requireContext(), "Chat deleted", Toast.LENGTH_SHORT).show();
                            requireActivity().onBackPressed();
                        })
                        .setNegativeButton("Cancel", null)
                        .show());

        view.findViewById(R.id.clearCacheButton).setOnClickListener(v -> {
            ChatCacheManager.getInstance(requireContext()).clearAll();
            Toast.makeText(requireContext(), "Cache cleared", Toast.LENGTH_SHORT).show();
        });

        sendButton.setOnClickListener(v -> sendMessage());
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        if (chatId == null) {
            Toast.makeText(requireContext(), "Error: No chat ID", Toast.LENGTH_SHORT).show();
            return;
        }

        messagesRef = FirebaseDatabase.getInstance()
                .getReference("chats").child(chatId).child("messages");
        loadCurrentUsername();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (chatId != null) {
            ChatCacheApplication.setCurrentOpenChatId(chatId);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        ChatCacheApplication.setCurrentOpenChatId(null);
        unregisterNetworkRecoveryCallback();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }

    private void loadCurrentUsername() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        if (currentUsername == null) {
            currentUsername = ChatCacheApplication.getCurrentUsername();
        }

        if (NetworkMonitor.getInstance(requireContext()).isConnected()) {
            String safeEmail = user.getEmail().replace(".", "_");
            FirebaseDatabase.getInstance()
                    .getReference("users").child(safeEmail)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            currentUsername = snapshot.child("username").getValue(String.class);
                            if (currentUsername != null) {
                                ChatCacheApplication.setCurrentUsername(currentUsername);
                            }

                            loadCachedMessages();
                            attachMessagesListener();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loadCachedMessages();
                            attachMessagesListener();
                        }
                    });
        } else {
            if (currentUsername == null) {
                currentUsername = "";
            }
            loadCachedMessages();
            registerNetworkRecoveryCallback();
        }
    }

    private void loadCachedMessages() {
        if (chatId != null && ChatCacheManager.isCachingEnabled()) {
            ArrayList<MessageModel> cached = ChatCacheManager.getInstance(requireContext())
                    .loadMessages(chatId);
            if (!cached.isEmpty()) {
                messagesList.clear();
                messagesList.addAll(cached);
                messageAdapter.notifyDataSetChanged();
                if (!messagesList.isEmpty()) {
                    messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
                }
            }
        }
    }

    private void registerNetworkRecoveryCallback() {
        networkRecoveryCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                unregisterNetworkRecoveryCallback();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> loadCurrentUsername());
                }
            }
        };
        NetworkMonitor.getInstance(requireContext()).registerNetworkCallback(networkRecoveryCallback);
    }

    private void unregisterNetworkRecoveryCallback() {
        if (networkRecoveryCallback != null) {
            NetworkMonitor.getInstance(requireContext()).unregisterNetworkCallback(networkRecoveryCallback);
            networkRecoveryCallback = null;
        }
    }

    private void attachMessagesListener() {
        if (messagesListener != null) return;

        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messagesList.clear();

                MessageModel previousMessage = null;
                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    String messageId = msgSnap.getKey();

                    String text           = msgSnap.child("text").getValue(String.class);
                    String senderUsername = msgSnap.child("senderUsername").getValue(String.class);
                    Long   timestamp      = msgSnap.child("timestamp").getValue(Long.class);
                    String imageUrl       = msgSnap.child("imageUrl").getValue(String.class);

                    if ((text == null || text.isEmpty()) && imageUrl == null) continue;
                    if (senderUsername == null) continue;

                    boolean sentByMe = currentUsername != null
                            && senderUsername.equals(currentUsername);

                    String textVal = text != null ? text : "";
                    MessageModel msg = new MessageModel(
                            textVal, senderUsername,
                            timestamp != null ? timestamp : 0L,
                            sentByMe);
                    msg.setMessageId(messageId);
                    msg.setImageUrl(imageUrl);

                    if (timestamp != null && timestamp > 0) {
                        if (previousMessage != null && previousMessage.getTimestamp() > 0) {
                            Calendar prevCal = Calendar.getInstance();
                            prevCal.setTimeInMillis(previousMessage.getTimestamp());

                            Calendar currCal = Calendar.getInstance();
                            currCal.setTimeInMillis(timestamp);

                            if (prevCal.get(Calendar.YEAR) != currCal.get(Calendar.YEAR) ||
                                    prevCal.get(Calendar.DAY_OF_YEAR) != currCal.get(Calendar.DAY_OF_YEAR)) {
                                MessageModel separator = new MessageModel(
                                        formatDateMessage(timestamp),
                                        "date_separator", timestamp, false);
                                messagesList.add(separator);
                            }
                        } else {
                            MessageModel separator = new MessageModel(
                                    formatDateMessage(timestamp),
                                    "date_separator", timestamp, false);
                            messagesList.add(separator);
                        }
                    }

                    messagesList.add(msg);
                    previousMessage = msg;
                }

                messageAdapter.notifyDataSetChanged();
                if (!messagesList.isEmpty()) {
                    messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
                }
                if (chatId != null) {
                    ChatCacheManager.getInstance(requireContext()).saveMessages(chatId, messagesList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        };

        messagesRef.addValueEventListener(messagesListener);
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty() || chatId == null || currentUsername == null) return;

        long now = System.currentTimeMillis();

        if (!NetworkMonitor.getInstance(requireContext()).isConnected()) {
            if (requireContext().getSharedPreferences("offline_snooze_prefs", Context.MODE_PRIVATE)
                    .getLong("snoozed_until", 0) > System.currentTimeMillis()) {
                String localId = "local_" + UUID.randomUUID().toString();
                MessageModel msg = new MessageModel(text, currentUsername, now, true);
                msg.setMessageId(localId);
                messagesList.add(msg);
                messageAdapter.notifyDataSetChanged();
                messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
                ChatCacheManager.getInstance(requireContext()).saveMessages(chatId, messagesList);

                ChatModel chat = new ChatModel();
                chat.setChatId(chatId);
                chat.setUsername(partnerUsername);
                chat.setLastMessage(currentUsername + ": " + text);
                chat.setTimestamp(now);
                if (currentUserSafeEmail != null) {
                    ChatMetadataManager.getInstance(requireContext()).saveChat(currentUserSafeEmail, chat);
                }

                messageInput.setText("");
                return;
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("No Internet Connection")
                    .setMessage("No internet connection, action will not be synced out of device.")
                    .setPositiveButton("OK", (d, w) -> {
                        String localId = "local_" + UUID.randomUUID().toString();
                        MessageModel msg = new MessageModel(text, currentUsername, now, true);
                        msg.setMessageId(localId);
                        messagesList.add(msg);
                        messageAdapter.notifyDataSetChanged();
                        messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
                        ChatCacheManager.getInstance(requireContext()).saveMessages(chatId, messagesList);

                        ChatModel chat = new ChatModel();
                        chat.setChatId(chatId);
                        chat.setUsername(partnerUsername);
                        chat.setLastMessage(currentUsername + ": " + text);
                        chat.setTimestamp(now);
                        if (currentUserSafeEmail != null) {
                            ChatMetadataManager.getInstance(requireContext()).saveChat(currentUserSafeEmail, chat);
                        }

                        messageInput.setText("");
                    })
                    .setNegativeButton("Don't show for 30 min", (d, w) -> {
                        requireContext().getSharedPreferences("offline_snooze_prefs", Context.MODE_PRIVATE)
                                .edit().putLong("snoozed_until", System.currentTimeMillis() + 1800000).apply();

                        String localId = "local_" + UUID.randomUUID().toString();
                        MessageModel msg = new MessageModel(text, currentUsername, now, true);
                        msg.setMessageId(localId);
                        messagesList.add(msg);
                        messageAdapter.notifyDataSetChanged();
                        messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
                        ChatCacheManager.getInstance(requireContext()).saveMessages(chatId, messagesList);

                        ChatModel chat = new ChatModel();
                        chat.setChatId(chatId);
                        chat.setUsername(partnerUsername);
                        chat.setLastMessage(currentUsername + ": " + text);
                        chat.setTimestamp(now);
                        if (currentUserSafeEmail != null) {
                            ChatMetadataManager.getInstance(requireContext()).saveChat(currentUserSafeEmail, chat);
                        }

                        messageInput.setText("");
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
            return;
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", text);
        messageData.put("senderUsername", currentUsername);
        messageData.put("timestamp", ServerValue.TIMESTAMP);

        messagesRef.push().setValue(messageData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                messageInput.setText("");

                Map<String, Object> lastMessageUpdate = new HashMap<>();
                lastMessageUpdate.put("lastMessage/text", text);
                lastMessageUpdate.put("lastMessage/senderUsername", currentUsername);
                lastMessageUpdate.put("lastMessage/timestamp", now);

                FirebaseDatabase.getInstance()
                        .getReference("chats").child(chatId)
                        .updateChildren(lastMessageUpdate);

                if (currentUserSafeEmail != null) {
                    FirebaseDatabase.getInstance()
                            .getReference("users").child(currentUserSafeEmail)
                            .child("chats").child(chatId)
                            .updateChildren(lastMessageUpdate);
                }
            } else {
                Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteMessage(MessageModel message) {
        if (message.getMessageId() == null || chatId == null) return;

        if (!NetworkMonitor.getInstance(requireContext()).isConnected()) {
            if (requireContext().getSharedPreferences("offline_snooze_prefs", Context.MODE_PRIVATE)
                    .getLong("snoozed_until", 0) > System.currentTimeMillis()) {
                messagesList.removeIf(m -> message.getMessageId() != null && message.getMessageId().equals(m.getMessageId()));
                messageAdapter.notifyDataSetChanged();
                ChatCacheManager.getInstance(requireContext()).saveMessages(chatId, messagesList);
                return;
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle("No Internet Connection")
                    .setMessage("No internet connection, action will not be synced out of device.")
                    .setPositiveButton("OK", (d, w) -> {
                        messagesList.removeIf(m -> message.getMessageId() != null && message.getMessageId().equals(m.getMessageId()));
                        messageAdapter.notifyDataSetChanged();
                        ChatCacheManager.getInstance(requireContext()).saveMessages(chatId, messagesList);
                    })
                    .setNegativeButton("Don't show for 30 min", (d, w) -> {
                        requireContext().getSharedPreferences("offline_snooze_prefs", Context.MODE_PRIVATE)
                                .edit().putLong("snoozed_until", System.currentTimeMillis() + 1800000).apply();
                        messagesList.removeIf(m -> message.getMessageId() != null && message.getMessageId().equals(m.getMessageId()));
                        messageAdapter.notifyDataSetChanged();
                        ChatCacheManager.getInstance(requireContext()).saveMessages(chatId, messagesList);
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
            return;
        }

        FirebaseDatabase.getInstance().getReference("chats")
                .child(chatId).child("messages")
                .child(message.getMessageId())
                .removeValue();
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        profileEmail.setText(user.getEmail());

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getEmail().replace(".", "_"))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("name").getValue(String.class);
                        String username = snapshot.child("username").getValue(String.class);
                        if (name != null) profileName.setText(name);
                        if (username != null) profileUsername.setText(username);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private String formatDateMessage(long timestamp) {
        Calendar cal = Calendar.getInstance();
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTimeInMillis(timestamp);

        int today = cal.get(Calendar.DAY_OF_YEAR);
        int msgDay = msgCal.get(Calendar.DAY_OF_YEAR);
        int year = cal.get(Calendar.YEAR);
        int msgYear = msgCal.get(Calendar.YEAR);

        if (year == msgYear && today == msgDay) return "Today";
        else if (year == msgYear && today - msgDay == 1) return "Yesterday";
        else return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(msgCal.getTime());
    }
}