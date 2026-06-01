package com.example.mainproj.messaging;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mainproj.Action;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

public class MessagingActivity extends AppCompatActivity implements MessageAdapter.OnMessageDeleteListener {

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

    // Callback that watches for network recovery while offline
    private ConnectivityManager.NetworkCallback networkRecoveryCallback;

    private ImageView profileImage;
    private TextView profileName, profileUsername, profileEmail;

    private TextView tvChatName;

    private File pendingImageFile;
    private LinearLayout photoPreviewContainer;
    private ImageView ivPhotoPreview;
    private ImageButton btnRemovePhoto;
    private ImageButton btnCamera;
    private ImageButton btnGallery;

    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatId = getIntent().getStringExtra("chatId");
        partnerUsername = getIntent().getStringExtra("username");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        drawerLayout = findViewById(R.id.chatDrawerLayout);

        tvChatName = findViewById(R.id.tvChatName);

        if (partnerUsername != null) {
            tvChatName.setText(partnerUsername);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);
        photoPreviewContainer = findViewById(R.id.photoPreviewContainer);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto);

        messagesList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messagesList, this);
        messageAdapter.setOnMessageDeleteListener(this);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && firebaseUser.getEmail() != null) {
            currentUserSafeEmail = firebaseUser.getEmail().replace(".", "_");
        }

        storageRef = FirebaseStorage.getInstance().getReference();

        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        profileUsername = findViewById(R.id.profileUsername);
        profileEmail = findViewById(R.id.profileEmail);

        loadUserProfile();

        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            SessionManager.clearAllPrefs(this);
            FirebaseAuth.getInstance().signOut();
            android.content.Intent intent = new android.content.Intent(this, Login_activity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        findViewById(R.id.clearChatsButton).setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Delete Chat")
                        .setMessage("Are you sure you want to delete this chat?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (!NetworkMonitor.getInstance(this).isConnected()) {
                                ChatCacheManager.getInstance(this).clearChat(chatId);
                                if (currentUserSafeEmail != null) {
                                    ChatMetadataManager.getInstance(this).clearChat(currentUserSafeEmail, chatId);
                                }
                                Action.enqueue(this, new Action("delete_chat", chatId, null, null));
                                Toast.makeText(this, "Chat deleted", Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }
                            ChatCacheManager.getInstance(this).clearAll();
                            if (chatId != null && currentUserSafeEmail != null) {
                                FirebaseDatabase.getInstance().getReference("users")
                                        .child(currentUserSafeEmail).child("chats").child(chatId)
                                        .removeValue();
                            }
                            Toast.makeText(this, "Chat deleted", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .setNegativeButton("Cancel", null)
                        .show());

        findViewById(R.id.clearCacheButton).setOnClickListener(v -> {
            ChatCacheManager.getInstance(this).clearAll();
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
        });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result && pendingImageFile != null && pendingImageFile.exists()) {
                        showPhotoPreview(pendingImageFile);
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        copyGalleryUriToCache(uri);
                    }
                });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        launchCameraInternal();
                    } else {
                        Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                    }
                });

        btnCamera.setOnClickListener(v -> openCamera());
        btnGallery.setOnClickListener(v -> openGallery());
        btnRemovePhoto.setOnClickListener(v -> removePhotoPreview());
        sendButton.setOnClickListener(v -> sendMessage());
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        if (chatId == null) {
            Toast.makeText(this, "Error: No chat ID", Toast.LENGTH_SHORT).show();
            return;
        }

        messagesRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages");
        loadMessages();
    }

    public void loadMessages() {
        // Resolve username: try cache first, then Firebase if online
        if (currentUsername == null) {
            currentUsername = ChatCacheApplication.getCurrentUsername();
        }

        if (currentUsername == null && NetworkMonitor.getInstance(this).isConnected()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                String safeEmail = user.getEmail().replace(".", "_");
                DatabaseReference userRef = FirebaseDatabase.getInstance()
                        .getReference("users").child(safeEmail);
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentUsername = snapshot.child("username").getValue(String.class);
                        if (currentUsername != null) {
                            ChatCacheApplication.setCurrentUsername(currentUsername);
                        }
                        loadMessages();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        currentUsername = "";
                        loadMessages();
                    }
                });
            }
            return;
        }

        if (currentUsername == null) {
            currentUsername = "";
        }

        if (NetworkMonitor.getInstance(this).isConnected()) {
            unregisterNetworkRecoveryCallback();

            if (chatId != null && ChatCacheManager.isCachingEnabled()) {
                ArrayList<MessageModel> cached =
                        ChatCacheManager.getInstance(this).loadMessages(chatId);
                if (!cached.isEmpty()) {
                    messagesList.clear();
                    messagesList.addAll(cached);
                    messageAdapter.notifyDataSetChanged();
                    messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
                }
            }

            attachMessagesListener();

        } else {
            if (chatId != null && ChatCacheManager.isCachingEnabled()) {
                ArrayList<MessageModel> cached =
                        ChatCacheManager.getInstance(this).loadMessages(chatId);
                if (!cached.isEmpty()) {
                    messagesList.clear();
                    messagesList.addAll(cached);
                    messageAdapter.notifyDataSetChanged();
                    messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
                }
            }

            networkRecoveryCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    unregisterNetworkRecoveryCallback();
                    runOnUiThread(() -> loadMessages());
                }
            };
            NetworkMonitor.getInstance(this).registerNetworkCallback(networkRecoveryCallback);
        }
    }

    private void unregisterNetworkRecoveryCallback() {
        if (networkRecoveryCallback != null) {
            NetworkMonitor.getInstance(this).unregisterNetworkCallback(networkRecoveryCallback);
            networkRecoveryCallback = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (chatId != null) {
            ChatCacheApplication.setCurrentOpenChatId(chatId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        ChatCacheApplication.setCurrentOpenChatId(null);
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
            messagesListener = null;
        }
        unregisterNetworkRecoveryCallback();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // ---------------------------------------------------------------------------
    // Firebase real-time listener
    // ---------------------------------------------------------------------------
    private void attachMessagesListener() {
        if (messagesListener != null) return;

        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messagesList.clear();

                MessageModel previousMessage = null;
                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    String messageId = msgSnap.getKey();

                    String text = msgSnap.child("text").getValue(String.class);
                    String senderUsername = msgSnap.child("senderUsername").getValue(String.class);
                    String time = msgSnap.child("time").getValue(String.class);
                    Long timestampLong = msgSnap.child("timestamp").getValue(Long.class);
                    String imageUrl = msgSnap.child("imageUrl").getValue(String.class);

                    if ((text == null || text.isEmpty()) && imageUrl == null) continue;
                    if (senderUsername == null) continue;

                    boolean sentByMe = currentUsername != null && senderUsername.equals(currentUsername);

                    String textVal = text != null ? text : "";
                    MessageModel msg = new MessageModel(
                            textVal, senderUsername,
                            time != null ? time : "",
                            timestampLong != null ? timestampLong : 0L,
                            sentByMe);
                    msg.setMessageId(messageId);
                    msg.setImageUrl(imageUrl);

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
                if (chatId != null) {
                    ChatCacheManager.getInstance(MessagingActivity.this).saveMessages(chatId, messagesList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MessagingActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        };

        messagesRef.addValueEventListener(messagesListener);
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (chatId == null || currentUsername == null) return;
        if (text.isEmpty() && pendingImageFile == null) return;

        if (pendingImageFile != null) {
            uploadPhotoAndSend(text);
        } else {
            sendTextMessageOnly(text);
        }
    }

    private void sendTextMessageOnly(String text) {
        if (chatId == null || currentUsername == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeStr = sdf.format(new Date());
        long now = System.currentTimeMillis();

        if (!NetworkMonitor.getInstance(this).isConnected()) {
            String localId = "local_" + UUID.randomUUID().toString();
            MessageModel msg = new MessageModel(text, currentUsername, timeStr, now, true);
            msg.setMessageId(localId);
            messagesList.add(msg);
            messageAdapter.notifyDataSetChanged();
            messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
            ChatCacheManager.getInstance(this).saveMessages(chatId, messagesList);

            ChatModel chat = new ChatModel();
            chat.setChatId(chatId);
            chat.setUsername(partnerUsername);
            chat.setLastMessage(currentUsername + ": " + text);
            chat.setTimestamp(now);
            if (currentUserSafeEmail != null) {
                ChatMetadataManager.getInstance(this).saveChat(currentUserSafeEmail, chat);
            }

            Map<String, Object> messageData = new HashMap<>();
            messageData.put("text", text);
            messageData.put("senderUsername", currentUsername);
            messageData.put("time", timeStr);
            String payloadJson;
            try {
                JSONObject payload = new JSONObject();
                for (Map.Entry<String, Object> e : messageData.entrySet()) {
                    payload.put(e.getKey(), e.getValue());
                }
                payloadJson = payload.toString();
            } catch (JSONException e) {
                payloadJson = "{}";
            }
            Action.enqueue(this, new Action("send_message", chatId, localId, payloadJson));

            Map<String, Object> headerUpdate = new HashMap<>();
            headerUpdate.put("lastMessage/text", text);
            headerUpdate.put("lastMessage/senderUsername", currentUsername);
            headerUpdate.put("lastMessage/timestamp", now);
            try {
                JSONObject headerPayload = new JSONObject();
                for (Map.Entry<String, Object> e : headerUpdate.entrySet()) {
                    headerPayload.put(e.getKey(), e.getValue());
                }
                Action.enqueue(this, new Action("update_chat_header", chatId, null, headerPayload.toString()));
            } catch (JSONException e) {
                // fall through
            }

            messageInput.setText("");
            return;
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", text);
        messageData.put("senderUsername", currentUsername);
        messageData.put("timestamp", ServerValue.TIMESTAMP);
        messageData.put("time", timeStr);

        messagesRef.push().setValue(messageData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                messageInput.setText("");

                Map<String, Object> lastMessageUpdate = new HashMap<>();
                lastMessageUpdate.put("lastMessage/text", text);
                lastMessageUpdate.put("lastMessage/senderUsername", currentUsername);
                lastMessageUpdate.put("lastMessage/timestamp", now);

                DatabaseReference chatRef = FirebaseDatabase.getInstance()
                        .getReference("chats").child(chatId);
                chatRef.updateChildren(lastMessageUpdate);

                if (currentUserSafeEmail != null) {
                    DatabaseReference userChatRef = FirebaseDatabase.getInstance()
                            .getReference("users").child(currentUserSafeEmail)
                            .child("chats").child(chatId);
                    userChatRef.updateChildren(lastMessageUpdate);
                }
            } else {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadPhotoAndSend(String caption) {
        if (pendingImageFile == null || chatId == null || currentUsername == null) return;

        if (!NetworkMonitor.getInstance(this).isConnected()) {
            Toast.makeText(this, "Cannot send images offline", Toast.LENGTH_SHORT).show();
            return;
        }

        sendButton.setEnabled(false);
        String fileName = "chat_" + chatId + "/" + UUID.randomUUID() + ".jpg";
        StorageReference fileRef = storageRef.child("chat_images").child(fileName);

        byte[] imageBytes = compressImage(pendingImageFile);
        if (imageBytes == null) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            sendButton.setEnabled(true);
            return;
        }

        fileRef.putBytes(imageBytes)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            sendImageMessage(caption, imageUrl);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
                            sendButton.setEnabled(true);
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    sendButton.setEnabled(true);
                });
    }

    private void sendImageMessage(String caption, String imageUrl) {
        String text = caption != null && !caption.isEmpty() ? caption : "";

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", text);
        messageData.put("senderUsername", currentUsername);
        messageData.put("timestamp", ServerValue.TIMESTAMP);
        messageData.put("imageUrl", imageUrl);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        messageData.put("time", sdf.format(new Date()));

        messagesRef.push().setValue(messageData).addOnCompleteListener(task -> {
            sendButton.setEnabled(true);
            if (task.isSuccessful()) {
                messageInput.setText("");
                removePhotoPreview();

                String displayText = text.isEmpty() ? "Photo" : text;
                long now = System.currentTimeMillis();
                Map<String, Object> lastMessageUpdate = new HashMap<>();
                lastMessageUpdate.put("lastMessage/text", displayText);
                lastMessageUpdate.put("lastMessage/senderUsername", currentUsername);
                lastMessageUpdate.put("lastMessage/timestamp", now);

                DatabaseReference chatRef = FirebaseDatabase.getInstance()
                        .getReference("chats").child(chatId);
                chatRef.updateChildren(lastMessageUpdate);

                if (currentUserSafeEmail != null) {
                    DatabaseReference userChatRef = FirebaseDatabase.getInstance()
                            .getReference("users").child(currentUserSafeEmail)
                            .child("chats").child(chatId);
                    userChatRef.updateChildren(lastMessageUpdate);
                }
            } else {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteMessage(MessageModel message) {
        if (message.getMessageId() == null || chatId == null) return;

        if (!NetworkMonitor.getInstance(this).isConnected()) {
            messagesList.removeIf(m -> message.getMessageId() != null && message.getMessageId().equals(m.getMessageId()));
            messageAdapter.notifyDataSetChanged();
            ChatCacheManager.getInstance(this).saveMessages(chatId, messagesList);
            Action.enqueue(this, new Action("delete_message", chatId, message.getMessageId(), null));
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

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getEmail().replace(".", "_"));

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String username = snapshot.child("username").getValue(String.class);
                if (name != null) profileName.setText(name);
                if (username != null) profileUsername.setText(username);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
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
        else
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(msgCal.getTime());
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCameraInternal();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCameraInternal() {
        File cameraDir = new File(getCacheDir(), "camera");
        cameraDir.mkdirs();
        pendingImageFile = new File(cameraDir, "IMG_" + System.currentTimeMillis() + ".jpg");
        Uri photoUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", pendingImageFile);
        cameraLauncher.launch(photoUri);
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void copyGalleryUriToCache(Uri sourceUri) {
        try {
            File galleryDir = new File(getCacheDir(), "gallery");
            galleryDir.mkdirs();
            pendingImageFile = new File(galleryDir, "IMG_" + System.currentTimeMillis() + ".jpg");

            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            FileOutputStream outputStream = new FileOutputStream(pendingImageFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            showPhotoPreview(pendingImageFile);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            pendingImageFile = null;
        }
    }

    private void showPhotoPreview(File photoFile) {
        Glide.with(this)
                .load(photoFile)
                .centerCrop()
                .into(ivPhotoPreview);
        photoPreviewContainer.setVisibility(View.VISIBLE);
    }

    private void removePhotoPreview() {
        photoPreviewContainer.setVisibility(View.GONE);
        if (pendingImageFile != null) {
            pendingImageFile.delete();
            pendingImageFile = null;
        }
    }

    private byte[] compressImage(File imageFile) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), opts);

            int maxDimension = 1024;
            int sampleSize = 1;
            while (opts.outWidth / sampleSize > maxDimension
                    || opts.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2;
            }   

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sampleSize;
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), opts);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            bitmap.recycle();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}