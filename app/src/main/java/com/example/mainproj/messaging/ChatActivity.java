package com.example.mainproj.messaging;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.BaseActivity;
import com.example.mainproj.R;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends BaseActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private ArrayList<MessageModel> messagesList;
    private EditText messageInput;
    private ImageButton sendButton;

    private String chatId;
    private String partnerUsername;
    private String partnerNickname;
    private String partnerBio;
    private String partnerLastSeen;
    private boolean partnerOnline;
    private String currentUsername;

    private DatabaseReference messagesRef;
    private ChildEventListener messagesListener;

    private CircleImageView profileImage;
    private TextView tvNickname;
    private TextView tvOnlineStatus;
    private TextView tvTypingIndicator;
    private ImageButton btnBack;
    private ImageButton btnMore;
    private ImageButton btnAttach;
    private ImageButton btnCamera;

    private GoogleSignInClient mGoogleSignInClient;
    private ChatCacheManager cacheManager;
    private Set<Long> loadedTimestamps = new HashSet<>();

    private Uri cameraImageUri;

    private final ActivityResultLauncher<Intent> filePreviewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String fileUriString = result.getData().getStringExtra(FilePreviewActivity.RESULT_FILE_URI);
                    if (fileUriString != null) {
                        Uri fileUri = Uri.parse(fileUriString);
                        sendImageMessage(fileUri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String imageUriString = result.getData().getStringExtra(CameraPreviewActivity.RESULT_IMAGE_URI);
                    if (imageUriString != null) {
                        Uri imageUri = Uri.parse(imageUriString);
                        sendImageMessage(imageUri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        Intent previewIntent = new Intent(this, FilePreviewActivity.class);
                        previewIntent.putExtra(FilePreviewActivity.EXTRA_FILE_URI, selectedUri.toString());
                        filePreviewLauncher.launch(previewIntent);
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initViews();

        partnerUsername = getIntent().getStringExtra("username");
        chatId = getIntent().getStringExtra("chatId");
        partnerNickname = getIntent().getStringExtra("nickname");
        partnerBio = getIntent().getStringExtra("bio");
        partnerLastSeen = getIntent().getStringExtra("lastSeen");
        partnerOnline = getIntent().getBooleanExtra("online", false);

        setupTopBar();
        setupMessageInput();
        setupRecyclerView();
        setupCache();
        loadCurrentUsername();

        if (chatId == null) {
            Toast.makeText(this, "Error: No chat ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        messagesRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages");
        messagesListener = buildMessagesListener();
    }

    private void initViews() {
        profileImage = findViewById(R.id.profileImage);
        tvNickname = findViewById(R.id.tvNickname);
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus);
        tvTypingIndicator = findViewById(R.id.tvTypingIndicator);
        btnBack = findViewById(R.id.btnBack);
        btnMore = findViewById(R.id.btnMore);
        btnAttach = findViewById(R.id.btnAttach);
        btnCamera = findViewById(R.id.btnCamera);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
    }

    private void setupTopBar() {
        btnBack.setOnClickListener(v -> finish());

        btnMore.setOnClickListener(v -> {
            openUserProfileBottomSheet();
        });

        profileImage.setOnClickListener(v -> {
            openUserProfileBottomSheet();
        });

        tvNickname.setText(partnerNickname != null ? partnerNickname : partnerUsername);
        updateOnlineStatus();
    }

    private void updateOnlineStatus() {
        if (partnerOnline) {
            tvOnlineStatus.setText("Online");
            tvOnlineStatus.setTextColor(getResources().getColor(R.color.colorPrimary, getTheme()));
        } else {
            tvOnlineStatus.setText(partnerLastSeen != null && !partnerLastSeen.isEmpty() 
                ? "Last seen " + partnerLastSeen 
                : "Offline");
            tvOnlineStatus.setTextColor(getResources().getColor(R.color.colorTextSecondary, getTheme()));
        }
    }

    private void openUserProfileBottomSheet() {
        if (partnerUsername == null) return;

        UserProfileBottomSheet bottomSheet = UserProfileBottomSheet.newInstance(
                partnerUsername,
                partnerBio != null ? partnerBio : "",
                partnerLastSeen != null ? partnerLastSeen : "",
                partnerOnline
        );

        bottomSheet.setActionListener(new UserProfileBottomSheet.ProfileActionListener() {
            @Override
            public void onSendMessage(String username) {
            }

            @Override
            public void onViewProfile(String username) {
            }

            @Override
            public void onBlockUser(String username) {
            }
        });

        bottomSheet.show(getSupportFragmentManager(), UserProfileBottomSheet.TAG);
    }

    private void setupMessageInput() {
        sendButton.setOnClickListener(v -> sendMessage());

        btnCamera.setOnClickListener(v -> checkCameraPermissionAndLaunch());

        btnAttach.setOnClickListener(v -> openFilePicker());
    }

    private void checkCameraPermissionAndLaunch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        photoFile
                );
                cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraLauncher.launch(cameraIntent);
            }
        }
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private void sendImageMessage(Uri imageUri) {
        if (chatId == null || currentUsername == null) return;

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        StorageReference imageRef = storageRef.child("chat_images").child(chatId).child(timeStamp + ".jpg");

        imageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("imageUrl", downloadUri.toString());
                messageData.put("senderUsername", currentUsername);
                messageData.put("timestamp", ServerValue.TIMESTAMP);

                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                messageData.put("time", sdf.format(new Date()));

                messagesRef.push().setValue(messageData).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> lastMessageUpdate = new HashMap<>();
                        lastMessageUpdate.put("lastMessage", "📷 Photo");
                        lastMessageUpdate.put("time", System.currentTimeMillis());
                        FirebaseDatabase.getInstance().getReference("chats").child(chatId).updateChildren(lastMessageUpdate);
                    } else {
                        Toast.makeText(ChatActivity.this, "Failed to send image", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(ChatActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        messagesList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messagesList, this);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void setupCache() {
        cacheManager = ChatCacheManager.getInstance(this);
        loadCachedMessages();

        if (chatId != null) {
            ChatCacheApplication.setCurrentOpenChatId(chatId);
        }
    }

    private void loadCurrentUsername() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty() || chatId == null || currentUsername == null) return;

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", text);
        messageData.put("senderUsername", currentUsername);
        messageData.put("timestamp", ServerValue.TIMESTAMP);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        messageData.put("time", sdf.format(new Date()));

        messagesRef.push().setValue(messageData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                messageInput.setText("");

                Map<String, Object> lastMessageUpdate = new HashMap<>();
                lastMessageUpdate.put("lastMessage", text);
                lastMessageUpdate.put("time", System.currentTimeMillis());

                FirebaseDatabase.getInstance().getReference("chats").child(chatId).updateChildren(lastMessageUpdate);
            } else {
                Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCachedMessages() {
        if (chatId == null) return;
        ArrayList<MessageModel> cached = cacheManager.loadMessages(chatId);
        for (MessageModel msg : cached) {
            messagesList.add(msg);
            loadedTimestamps.add(msg.getTimestampLong());
        }
        
        if (!messagesList.isEmpty()) {
            rebuildDateSeparators();
            messageAdapter.notifyDataSetChanged();
            messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
        }
    }

    private void rebuildDateSeparators() {
        if (messagesList.isEmpty()) return;
        
        List<MessageModel> newList = new ArrayList<>();
        long lastDateTimestamp = 0;
        
        for (MessageModel msg : messagesList) {
            if (msg.isDateSeparator()) continue;
            
            long msgTime = msg.getTimestampLong();
            
            Calendar msgCal = Calendar.getInstance();
            msgCal.setTimeInMillis(msgTime);
            Calendar lastCal = Calendar.getInstance();
            lastCal.setTimeInMillis(lastDateTimestamp);
            
            if (lastDateTimestamp == 0 || !isSameDay(msgCal, lastCal)) {
                newList.add(MessageModel.createDateSeparator(formatDateLabel(msgTime), msgTime));
                lastDateTimestamp = msgTime;
            }
            
            newList.add(msg);
        }
        
        messagesList.clear();
        messagesList.addAll(newList);
    }

    private void saveToCache() {
        if (chatId != null) {
            cacheManager.saveMessages(chatId, messagesList);
        }
    }

    private String formatDateLabel(long timestamp) {
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTimeInMillis(timestamp);
        
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        
        if (isSameDay(msgCal, today)) {
            return "Today";
        } else if (isSameDay(msgCal, yesterday)) {
            return "Yesterday";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) 
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void insertDateSeparatorIfNeeded(long newTimestamp, int insertPosition) {
        if (insertPosition == 0) {
            messagesList.add(insertPosition, MessageModel.createDateSeparator(formatDateLabel(newTimestamp), newTimestamp));
            messageAdapter.notifyItemInserted(insertPosition);
            return;
        }

        MessageModel prevMsg = messagesList.get(insertPosition - 1);
        if (prevMsg.isDateSeparator()) return;

        long prevTimestamp = prevMsg.getTimestampLong();
        
        Calendar prevCal = Calendar.getInstance();
        prevCal.setTimeInMillis(prevTimestamp);
        Calendar newCal = Calendar.getInstance();
        newCal.setTimeInMillis(newTimestamp);
        
        if (!isSameDay(prevCal, newCal)) {
            messagesList.add(insertPosition, MessageModel.createDateSeparator(formatDateLabel(newTimestamp), newTimestamp));
            messageAdapter.notifyItemInserted(insertPosition);
        }
    }

    private ChildEventListener buildMessagesListener() {
        return new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String text = snapshot.child("text").getValue(String.class);
                String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                String senderUsername = snapshot.child("senderUsername").getValue(String.class);
                String time = snapshot.child("time").getValue(String.class);
                Long timestampLong = snapshot.child("timestamp").getValue(Long.class);

                if ((text != null || imageUrl != null) && senderUsername != null && timestampLong != null) {
                    if (loadedTimestamps.contains(timestampLong)) {
                        return;
                    }
                    loadedTimestamps.add(timestampLong);
                    boolean sentByMe = senderUsername.equals(currentUsername);
                    String messageText = text != null ? text : "📷 Photo";
                    
                    int insertPosition = messagesList.size();
                    messagesList.add(new MessageModel(messageText, senderUsername, time, timestampLong, sentByMe));
                    
                    insertDateSeparatorIfNeeded(timestampLong, insertPosition);
                    messageAdapter.notifyDataSetChanged();
                    messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (chatId != null) {
            ChatCacheApplication.setCurrentOpenChatId(chatId);
            clearUnreadCount();
        }
        if (messagesRef != null && messagesListener != null) {
            messagesRef.addChildEventListener(messagesListener);
        }
    }

    private void clearUnreadCount() {
        if (chatId == null || currentUsername == null) return;
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("unread/" + currentUsername, 0);
        chatRef.updateChildren(updates);
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveToCache();
        ChatCacheApplication.setCurrentOpenChatId(null);
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }
}
