package com.example.mainproj.messaging;

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
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }

    private void loadCurrentUsername() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

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
                        attachMessagesListener();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
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
                    String time           = msgSnap.child("time").getValue(String.class);
                    Long   timestampLong  = msgSnap.child("timestamp").getValue(Long.class);

                    if (text == null || senderUsername == null) continue;

                    boolean sentByMe = currentUsername != null
                            && senderUsername.equals(currentUsername);

                    MessageModel msg = new MessageModel(
                            text, senderUsername,
                            time != null ? time : "",
                            timestampLong != null ? timestampLong : 0L,
                            sentByMe);
                    msg.setMessageId(messageId);

                    if (timestampLong != null && timestampLong > 0) {
                        if (previousMessage != null && previousMessage.getTimestampLong() > 0) {
                            Calendar prevCal = Calendar.getInstance();
                            prevCal.setTimeInMillis(previousMessage.getTimestampLong());

                            Calendar currCal = Calendar.getInstance();
                            currCal.setTimeInMillis(timestampLong);

                            if (prevCal.get(Calendar.YEAR) != currCal.get(Calendar.YEAR) ||
                                    prevCal.get(Calendar.DAY_OF_YEAR) != currCal.get(Calendar.DAY_OF_YEAR)) {
                                MessageModel separator = new MessageModel(
                                        formatDateMessage(timestampLong),
                                        "date_separator", "", timestampLong, false);
                                messagesList.add(separator);
                            }
                        } else {
                            MessageModel separator = new MessageModel(
                                    formatDateMessage(timestampLong),
                                    "date_separator", "", timestampLong, false);
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

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", text);
        messageData.put("senderUsername", currentUsername);
        messageData.put("timestamp", ServerValue.TIMESTAMP);
        messageData.put("time", sdf.format(new Date()));

        messagesRef.push().setValue(messageData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                messageInput.setText("");

                long now = System.currentTimeMillis();
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
        if (message.getMessageId() != null && chatId != null) {
            FirebaseDatabase.getInstance().getReference("chats")
                    .child(chatId).child("messages")
                    .child(message.getMessageId())
                    .removeValue();
        }
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