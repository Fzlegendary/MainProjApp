package com.example.mainproj.userSystem;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mainproj.R;
import com.example.mainproj.admin.AdminGatePermissionSheet;
import com.example.mainproj.auth.SessionManager;
import com.example.mainproj.messaging.ChatUtils;
import com.example.mainproj.messaging.MessagingActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USERNAME = "username";
    public static final String EXTRA_SAFE_EMAIL = "safeEmail";
    public static final String EXTRA_ROLE_LEVEL = "roleLevel";

    private MaterialToolbar toolbar;
    private ImageView ivProfilePicture;
    private TextView tvUsername, tvEmail, tvPhone, tvRoleBadge, tvStatus, tvBio;
    private TextView tvJoinedDate, tvMessageCount, tvFriendsCount;
    private View statusDot;
    private MaterialButton btnSendMessage, btnViewFriends;
    private LinearLayout adminSection;
    private MaterialButton btnManageRole, btnManageGatePerms;

    private String username;
    private String safeEmail;
    private int roleLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        username = getIntent().getStringExtra(EXTRA_USERNAME);
        safeEmail = getIntent().getStringExtra(EXTRA_SAFE_EMAIL);
        roleLevel = getIntent().getIntExtra(EXTRA_ROLE_LEVEL, -1);

        toolbar = findViewById(R.id.toolbar);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        tvStatus = findViewById(R.id.tvStatus);
        statusDot = findViewById(R.id.statusDot);
        tvBio = findViewById(R.id.tvBio);
        tvJoinedDate = findViewById(R.id.tvJoinedDate);
        tvMessageCount = findViewById(R.id.tvMessageCount);
        tvFriendsCount = findViewById(R.id.tvFriendsCount);
        btnViewFriends = findViewById(R.id.btnViewFriends);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        adminSection = findViewById(R.id.adminSection);
        btnManageRole = findViewById(R.id.btnManageRole);
        btnManageGatePerms = findViewById(R.id.btnManageGatePerms);

        toolbar.setNavigationOnClickListener(v -> finish());

        if (username != null) {
            toolbar.setTitle(username);
        }

        String myUsername1 = SessionManager.getCurrentUsername();
        boolean isSelf = username != null && myUsername1 != null && username.equalsIgnoreCase(myUsername1);
        if (isSelf) {
            btnSendMessage.setVisibility(View.GONE);
        }

        btnSendMessage.setOnClickListener(v -> {
            if (username == null || safeEmail == null) return;

            String mySafeEmail = SessionManager.getCurrentSafeEmail();
            String myUsername = SessionManager.getCurrentUsername();
            if (mySafeEmail == null || mySafeEmail.isEmpty() || myUsername == null || myUsername.isEmpty()) {
                FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
                if (fu != null && fu.getEmail() != null && fu.getDisplayName() != null) {
                    mySafeEmail = fu.getEmail().replace(".", "_");
                    myUsername = fu.getDisplayName();
                }
            }
            if (mySafeEmail == null || myUsername == null) return;

            String chatId = ChatUtils.generateChatId(myUsername, username);

            FirebaseDatabase.getInstance()
                    .getReference("users").child(mySafeEmail).child("chats").child(chatId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            Intent intent = new Intent(UserProfileActivity.this, MessagingActivity.class);
                            intent.putExtra("username", username);
                            intent.putExtra("safeEmail", safeEmail);
                            if (snap.exists()) {
                                intent.putExtra("chatId", chatId);
                            }
                            startActivity(intent);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Intent intent = new Intent(UserProfileActivity.this, MessagingActivity.class);
                            intent.putExtra("username", username);
                            intent.putExtra("safeEmail", safeEmail);
                            startActivity(intent);
                        }
                    });
        });

        if (SessionManager.isAdmin()) {
            adminSection.setVisibility(View.VISIBLE);

            btnManageRole.setOnClickListener(v -> {
                if (safeEmail != null && !safeEmail.isEmpty()) {
                    showRoleChangeDialog();
                } else {
                    Toast.makeText(this, "Cannot manage role \u2014 no email on record", Toast.LENGTH_SHORT).show();
                }
            });

            btnManageGatePerms.setOnClickListener(v -> {
                if (safeEmail != null && !safeEmail.isEmpty()) {
                    AdminGatePermissionSheet sheet = AdminGatePermissionSheet.newInstance("", 0, 0, false);
                    sheet.show(getSupportFragmentManager(), AdminGatePermissionSheet.TAG);
                } else {
                    Toast.makeText(this, "Cannot manage permissions \u2014 no email on record", Toast.LENGTH_SHORT).show();
                }
            });
        }

        loadUserData();
    }

    private void loadUserData() {
        if (safeEmail == null || safeEmail.isEmpty()) {
            tvUsername.setText(username != null ? username : "Unknown");
            setRoleBadge(roleLevel);
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(safeEmail);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String un = snapshot.child("username").getValue(String.class);
                if (un != null) {
                    tvUsername.setText(un);
                    toolbar.setTitle(un);
                } else {
                    tvUsername.setText(username != null ? username : "Unknown");
                }

                String email = snapshot.child("email").getValue(String.class);
                tvEmail.setText(email != null ? email : "");

                String phone = snapshot.child("phone").getValue(String.class);
                if (phone != null && !phone.isEmpty()) {
                    tvPhone.setText(phone);
                    tvPhone.setVisibility(View.VISIBLE);
                } else {
                    tvPhone.setVisibility(View.GONE);
                }

                String bio = snapshot.child("bio").getValue(String.class);
                tvBio.setText(bio != null && !bio.isEmpty() ? bio : "No bio yet.");

                String imgUrl = snapshot.child("profileImageUrl").getValue(String.class);
                if (imgUrl != null && !imgUrl.isEmpty()) {
                    com.bumptech.glide.Glide.with(UserProfileActivity.this)
                            .load(imgUrl)
                            .apply(com.bumptech.glide.request.RequestOptions.circleCropTransform()
                                    .placeholder(R.drawable.bg_avatar_circle)
                                    .error(R.drawable.bg_avatar_circle))
                            .into(ivProfilePicture);
                }

                Integer rl = snapshot.child("roleLevel").getValue(Integer.class);
                if (rl != null) roleLevel = rl;
                setRoleBadge(roleLevel);

                Boolean online = snapshot.child("online").getValue(Boolean.class);
                if (online != null && online) {
                    tvStatus.setText("Online");
                    statusDot.setVisibility(View.VISIBLE);
                } else {
                    Long ls = snapshot.child("lastSeen").getValue(Long.class);
                    tvStatus.setText(formatLastSeen(ls != null ? ls : 0L));
                    statusDot.setVisibility(View.INVISIBLE);
                }

                Long ca = snapshot.child("createdAt").getValue(Long.class);
                if (ca == null || ca <= 0) {
                    FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (fUser != null) ca = fUser.getMetadata().getCreationTimestamp();
                }
                if (ca != null && ca > 0) {
                    String date = new SimpleDateFormat("MMM yyyy", Locale.getDefault())
                            .format(new Date(ca));
                    tvJoinedDate.setText("Joined: " + date);
                }

                Long mc = snapshot.child("messageCount").getValue(Long.class);
                tvMessageCount.setText("Messages: " + (mc != null ? mc : 0));

                if (safeEmail != null) {
                    FirebaseDatabase.getInstance()
                            .getReference("users").child(safeEmail).child("friends")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snap) {
                                    long count = snap.getChildrenCount();
                                    tvFriendsCount.setText("Friends: " + count);
                                    btnViewFriends.setOnClickListener(v -> {
                                        List<String> names = new ArrayList<>();
                                        for (DataSnapshot child : snap.getChildren()) {
                                            if (child.getKey() != null) names.add(child.getKey());
                                        }
                                        if (names.isEmpty()) {
                                            new AlertDialog.Builder(UserProfileActivity.this)
                                                    .setTitle("Friends")
                                                    .setMessage("No friends yet.")
                                                    .setPositiveButton("OK", null).show();
                                        } else {
                                            new AlertDialog.Builder(UserProfileActivity.this)
                                                    .setTitle("Friends")
                                                    .setItems(names.toArray(new String[0]), null)
                                                    .setPositiveButton("Close", null).show();
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvUsername.setText(username != null ? username : "Unknown");
                setRoleBadge(roleLevel);
            }
        });
    }

    private void setRoleBadge(int level) {
        String text;
        int color;
        switch (level) {
            case 0:
                text = "User";
                color = ContextCompat.getColor(this, R.color.colorBadgeGray);
                break;
            case 1:
                text = "Admin";
                color = ContextCompat.getColor(this, R.color.colorPrimary);
                break;
            default:
                text = "Super Admin";
                color = ContextCompat.getColor(this, R.color.colorWarning);
                break;
        }
        tvRoleBadge.setText(text);
        GradientDrawable bg = (GradientDrawable) tvRoleBadge.getBackground().mutate();
        bg.setColor(color);
    }

    private void showRoleChangeDialog() {
        if (!SessionManager.canModifyUser(roleLevel)) {
            Toast.makeText(this, "Cannot modify this user's role", Toast.LENGTH_SHORT).show();
            return;
        }

        int myLevel = SessionManager.getRoleLevel();
        List<String> optionNames = new ArrayList<>();
        List<Integer> optionLevels = new ArrayList<>();
        for (int lvl = 0; lvl < myLevel; lvl++) {
            switch (lvl) {
                case 0: optionNames.add("User"); optionLevels.add(0); break;
                case 1: optionNames.add("Admin"); optionLevels.add(1); break;
                case 2: optionNames.add("Super Admin"); optionLevels.add(2); break;
            }
        }
        String[] options = optionNames.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Change Role - " + username)
                .setItems(options, (dialog, which) -> {
                    String newRole;
                    int newLevel = optionLevels.get(which);
                    switch (newLevel) {
                        case 0: newRole = "user"; break;
                        case 1: newRole = "admin"; break;
                        case 2: newRole = "superadmin"; break;
                        default: return;
                    }
                    SessionManager.setUserRole(safeEmail, newRole, newLevel);
                    Toast.makeText(this, "Role updated to " + newRole, Toast.LENGTH_SHORT).show();
                    roleLevel = newLevel;
                    setRoleBadge(newLevel);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatLastSeen(long timestamp) {
        if (timestamp <= 0) return "Last seen recently";
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        if (diff < 60_000) return "Just now";
        if (diff < 3_600_000) return (diff / 60_000) + "m ago";
        if (diff < 86_400_000) return (diff / 3_600_000) + "h ago";
        if (diff < 604_800_000) return (diff / 86_400_000) + " days ago";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }
}
