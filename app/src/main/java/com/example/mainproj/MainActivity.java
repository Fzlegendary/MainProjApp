package com.example.mainproj;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.example.mainproj.adapters.MainPagerAdapter;
import com.example.mainproj.auth.SessionManager;
import com.example.mainproj.fragments.ProfileFragment;
import com.example.mainproj.messaging.ChatCacheApplication;
import com.example.mainproj.messaging.ChatCacheManager;
import com.example.mainproj.messaging.ChatMetadataManager;
import com.example.mainproj.messaging.ChatModel;
import com.example.mainproj.messaging.MessageModel;
import com.example.mainproj.messaging.NetworkMonitor;
import com.example.mainproj.userSystem.Login_activity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends BaseActivity {

    private static final String PREFS_NAME = "MainProjPrefs";
    private static final String KEY_LOGGED_IN_EMAIL = "logged_in_email";

    private ViewPager viewPager;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private MainPagerAdapter pagerAdapter;
    private boolean isInSettingsNav = false;
    private ConnectivityManager.NetworkCallback offlineDialogCallback;
    private AlertDialog offlineDialog;
    private boolean wasOfflineDialogShown = false;
    private boolean isCaching = false;
    private final Handler connectivityHandler = new Handler(Looper.getMainLooper());
    private final Runnable connectivityCheckRunnable = () -> checkConnectivityAndShowDialog();

    private ImageView profileImage;
    private TextView profileName, profileUsername, profileEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        mAuth = FirebaseAuth.getInstance();

        requestNotificationPermission();

        ChatCacheManager.getInstance(this).setCachingEnabled(true);
        ChatMetadataManager.getInstance(this).setCachingEnabled(true);
        cacheAllData();

        setupViewPager();
        setupBottomNav();

        int tabPosition = getIntent().getIntExtra("tab_position", 1);
        viewPager.setCurrentItem(tabPosition, false);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, 0);
                    return insets;
                });

        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        profileUsername = findViewById(R.id.profileUsername);
        profileEmail = findViewById(R.id.profileEmail);

        MaterialButton logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            SessionManager.clearAllPrefs(this);
            mAuth.signOut();
            Intent intent = new Intent(this, Login_activity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        loadUserProfile();

        checkConnectivityAndShowDialog();

        ConnectivityManager.NetworkCallback cb = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    checkConnectivityAndShowDialog();
                    cacheAllData();
                    Action.syncAll(MainActivity.this);
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    checkConnectivityAndShowDialog();
                    connectivityHandler.removeCallbacks(connectivityCheckRunnable);
                    connectivityHandler.postDelayed(connectivityCheckRunnable, 2000);
                });
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                runOnUiThread(() -> checkConnectivityAndShowDialog());
            }

            @Override
            public void onUnavailable() {
                runOnUiThread(() -> checkConnectivityAndShowDialog());
            }
        };
        NetworkMonitor.getInstance(this).registerNetworkCallback(cb);
        offlineDialogCallback = cb;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkConnectivityAndShowDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        connectivityHandler.removeCallbacks(connectivityCheckRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (offlineDialogCallback != null) {
            NetworkMonitor.getInstance(this).unregisterNetworkCallback(offlineDialogCallback);
        }
    }

    private void cacheAllData() {
        if (isCaching) return;
        if (!NetworkMonitor.getInstance(this).isConnected()) return;

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || firebaseUser.getEmail() == null) return;

        isCaching = true;
        String safeEmail = firebaseUser.getEmail().replace(".", "_");

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(safeEmail);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = snapshot.child("username").getValue(String.class);
                if (username != null) {
                    ChatCacheApplication.setCurrentUsername(username);
                }

                // Cache all chat metadata
                DatabaseReference chatsRef = FirebaseDatabase.getInstance()
                        .getReference("users").child(safeEmail).child("chats");

                chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot chatSnapshot) {
                        for (DataSnapshot chatSnap : chatSnapshot.getChildren()) {
                            String chatId = chatSnap.getKey();
                            if (chatId == null) continue;

                            ChatModel chat = new ChatModel();
                            chat.setChatId(chatId);
                            chat.setUsername(chatSnap.child("username").getValue(String.class));
                            chat.setNickname(chatSnap.child("nickname").getValue(String.class));
                            chat.setImageUrl(chatSnap.child("imageUrl").getValue(String.class));
                            chat.setOnline(Boolean.TRUE.equals(
                                    chatSnap.child("online").getValue(Boolean.class)));
                            chat.setUnreadCount(chatSnap.child("unreadCount").getValue(Long.class));

                            String lastText = chatSnap.child("lastMessage")
                                    .child("text").getValue(String.class);
                            String sender = chatSnap.child("lastMessage")
                                    .child("senderUsername").getValue(String.class);
                            String currentUsername = ChatCacheApplication.getCurrentUsername();
                            boolean isMe = sender != null && sender.equals(currentUsername);
                            String prefix = isMe ? "You"
                                    : (sender != null ? sender : "");
                            chat.setLastMessage(prefix + ": "
                                    + (lastText != null ? lastText : ""));

                            Long ts = chatSnap.child("lastMessage")
                                    .child("timestamp").getValue(Long.class);
                            if (ts != null) chat.setTimestamp(ts);

                            String chatName = chatSnap.child("chatName")
                                    .getValue(String.class);
                            if (chatName != null && !chatName.isEmpty()) {
                                chat.setNickname(chatName);
                            }

                            ChatMetadataManager.getInstance(MainActivity.this)
                                    .saveChat(safeEmail, chat);

                            // Cache messages for this chat
                            DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                                    .getReference("chats").child(chatId).child("messages");

                            messagesRef.addListenerForSingleValueEvent(
                                    new ValueEventListener() {
                                @Override
                                public void onDataChange(
                                        @NonNull DataSnapshot msgSnapshot) {
                                    ArrayList<MessageModel> messages =
                                            new ArrayList<>();
                                    MessageModel previousMessage = null;
                                    for (DataSnapshot msgSnap :
                                            msgSnapshot.getChildren()) {
                                        String text = msgSnap.child("text")
                                                .getValue(String.class);
                                        String sender = msgSnap
                                                .child("senderUsername")
                                                .getValue(String.class);
                                        String time = msgSnap.child("time")
                                                .getValue(String.class);
                                        Long tsLong = msgSnap.child("timestamp")
                                                .getValue(Long.class);
                                        String imageUrl = msgSnap
                                                .child("imageUrl")
                                                .getValue(String.class);

                                        if (sender == null) continue;

                                        boolean sentByMe = currentUsername != null
                                                && sender.equals(currentUsername);

                                        if (tsLong != null && tsLong > 0) {
                                            if (previousMessage != null
                                                    && previousMessage
                                                            .getTimestampLong()
                                                            > 0) {
                                                Calendar prevCal =
                                                        Calendar.getInstance();
                                                prevCal.setTimeInMillis(
                                                        previousMessage
                                                                .getTimestampLong());
                                                Calendar currCal =
                                                        Calendar.getInstance();
                                                currCal.setTimeInMillis(tsLong);
                                                if (prevCal.get(
                                                        Calendar.YEAR)
                                                        != currCal.get(
                                                                Calendar.YEAR)
                                                        || prevCal.get(
                                                                Calendar
                                                                        .DAY_OF_YEAR)
                                                                != currCal.get(
                                                                        Calendar
                                                                                .DAY_OF_YEAR)) {
                                                    messages.add(
                                                            new MessageModel(
                                                                    formatDateMessage(
                                                                            tsLong),
                                                                    "date_separator",
                                                                    "",
                                                                    tsLong,
                                                                    false));
                                                }
                                            } else {
                                                messages.add(
                                                        new MessageModel(
                                                                formatDateMessage(
                                                                        tsLong),
                                                                "date_separator",
                                                                "",
                                                                tsLong,
                                                                false));
                                            }
                                        }

                                        MessageModel msg = new MessageModel(
                                                text != null ? text : "",
                                                sender,
                                                time != null ? time : "",
                                                tsLong != null ? tsLong : 0L,
                                                sentByMe);
                                        msg.setMessageId(msgSnap.getKey());
                                        msg.setImageUrl(imageUrl);
                                        messages.add(msg);
                                        previousMessage = msg;
                                    }
                                    ChatCacheManager.getInstance(
                                            MainActivity.this)
                                            .saveMessages(chatId, messages);
                                }

                                @Override
                                public void onCancelled(
                                        @NonNull DatabaseError error) {}
                            });
                        }
                        isCaching = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        isCaching = false;
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isCaching = false;
            }
        });
    }

    private void checkConnectivityAndShowDialog() {
        if (!NetworkMonitor.getInstance(this).isConnected()) {
            if (!wasOfflineDialogShown) {
                wasOfflineDialogShown = true;
                offlineDialog = new AlertDialog.Builder(this)
                        .setTitle("No Connection")
                        .setMessage("not connected to internet, certain features unavailable.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        } else {
            wasOfflineDialogShown = false;
            if (offlineDialog != null && offlineDialog.isShowing()) {
                offlineDialog.dismiss();
                offlineDialog = null;
            }
        }
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

    private void setupViewPager() {
        viewPager = findViewById(R.id.viewPager);
        pagerAdapter = new MainPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                updateBottomNavSelection(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                viewPager.setCurrentItem(0, true);
                return true;
            } else if (itemId == R.id.nav_messages) {
                viewPager.setCurrentItem(1, true);
                return true;
            } else if (itemId == R.id.nav_profile) {
                ProfileFragment pf = getProfileFragment();
                if (pf != null) {
                    if (isInSettingsNav) {
                        pf.showProfile();
                        isInSettingsNav = false;
                    } else {
                        pf.showSettingsInstead();
                        isInSettingsNav = true;
                    }
                }
                viewPager.setCurrentItem(2, true);
                return true;
            }
            return false;
        });
    }

    private void updateBottomNavSelection(int position) {
        switch (position) {
            case 0:
                bottomNav.setSelectedItemId(R.id.nav_home);
                isInSettingsNav = false;
                break;
            case 1:
                bottomNav.setSelectedItemId(R.id.nav_messages);
                isInSettingsNav = false;
                break;
            case 2:
                bottomNav.setSelectedItemId(R.id.nav_profile);
                break;
        }
    }

    private ProfileFragment getProfileFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.viewPager);
        if (currentFragment instanceof ProfileFragment) {
            return (ProfileFragment) currentFragment;
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 2 && isInSettingsNav) {
            ProfileFragment pf = getProfileFragment();
            if (pf != null) {
                pf.showProfile();
                isInSettingsNav = false;
            }
        } else {
            super.onBackPressed();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            } else {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission already granted");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            Log.d("MainActivity", "POST_NOTIFICATIONS permission result: granted=" + granted);
        }
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
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

}
