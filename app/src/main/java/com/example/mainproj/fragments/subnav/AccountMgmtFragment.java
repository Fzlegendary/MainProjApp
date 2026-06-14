package com.example.mainproj.fragments.subnav;

import android.app.AlertDialog;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.example.mainproj.adapters.UserManagementAdapter;
import com.example.mainproj.auth.SessionManager;
import com.example.mainproj.messaging.AdminNotificationHelper;
import com.example.mainproj.userSystem.UserProfileActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountMgmtFragment extends Fragment {

    private static final String TAG = "AccountMgmtFragment";
    private static final long UPDATE_INTERVAL_MS = 30000;

    // Pending deletions
    private Button btnPendingDeletions;
    private DatabaseReference listsRef;
    private ValueEventListener expiredListener;
    private List<PendingItem> pendingItems = new ArrayList<>();
    private Handler countdownHandler = new Handler(Looper.getMainLooper());
    private AlertDialog activeDialog;
    private LinearLayout dialogContainer;
    private Runnable countdownRunnable;

    // User management
    private View accessDenied, contentSection, loadingState, emptyState;
    private EditText searchInput;
    private ChipGroup chipGroup;
    private RecyclerView recyclerView;
    private MaterialButton btnSortName, btnSortRole, btnSortNewest;

    private UserManagementAdapter adapter;
    private List<UserManagementAdapter.UserListItem> allUsers = new ArrayList<>();
    private List<UserManagementAdapter.UserListItem> filteredUsers = new ArrayList<>();

    private ChildEventListener usersListener;
    private DatabaseReference usersRef;

    private String currentQuery = "";
    private int currentRoleFilter = -1;
    private SortOption currentSort = SortOption.NAME;

    private enum SortOption { NAME, ROLE_LEVEL, NEWEST }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sub_account_mgmt, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        accessDenied = view.findViewById(R.id.accessDenied);
        contentSection = view.findViewById(R.id.contentSection);
        searchInput = view.findViewById(R.id.searchInput);
        chipGroup = view.findViewById(R.id.chipGroup);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        loadingState = view.findViewById(R.id.loadingState);
        btnSortName = view.findViewById(R.id.btnSortName);
        btnSortRole = view.findViewById(R.id.btnSortRole);
        btnSortNewest = view.findViewById(R.id.btnSortNewest);
        btnPendingDeletions = view.findViewById(R.id.btnPendingDeletions);

        // Admin guard
        if (!SessionManager.isAdmin()) {
            accessDenied.setVisibility(View.VISIBLE);
            contentSection.setVisibility(View.GONE);
            return;
        }

        contentSection.setVisibility(View.VISIBLE);

        // Set up pending deletions — listen on /lists/ for expired items
        btnPendingDeletions.setOnClickListener(v -> showPendingDeletionsDialog());
        listsRef = FirebaseDatabase.getInstance().getReference("lists");
        expiredListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingItems.clear();
                long now = System.currentTimeMillis();
                for (DataSnapshot listTypeSnap : snapshot.getChildren()) {
                    String listType = listTypeSnap.getKey();
                    for (DataSnapshot itemSnap : listTypeSnap.getChildren()) {
                        String pushId = itemSnap.getKey();
                        String name = itemSnap.child("name").getValue(String.class);
                        Boolean isExpired = itemSnap.child("isExpired").getValue(Boolean.class);
                        Long deleteAt = itemSnap.child("deleteAt").getValue(Long.class);
                        Long expiresAt = itemSnap.child("timestampExpiresAt").getValue(Long.class);
                        if (pushId != null && name != null
                                && isExpired != null && isExpired
                                && deleteAt != null && deleteAt > now) {
                            pendingItems.add(new PendingItem(
                                    pushId, listType, name, deleteAt,
                                    expiresAt != null ? expiresAt : 0L));
                        }
                    }
                }
                updatePendingButton();
                Log.d(TAG, "Pending items updated, count=" + pendingItems.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "Firebase error: " + error.getMessage());
            }
        };
        listsRef.addValueEventListener(expiredListener);

        // Set up user management
        adapter = new UserManagementAdapter(requireContext(), filteredUsers, user -> {
            Intent intent = new Intent(requireContext(), UserProfileActivity.class);
            intent.putExtra(UserProfileActivity.EXTRA_USERNAME, user.username);
            intent.putExtra(UserProfileActivity.EXTRA_SAFE_EMAIL, user.safeEmail);
            intent.putExtra(UserProfileActivity.EXTRA_ROLE_LEVEL, user.roleLevel);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().toLowerCase();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentRoleFilter = -1;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipUser) currentRoleFilter = 0;
                else if (id == R.id.chipAdmin) currentRoleFilter = 1;
                else if (id == R.id.chipSuperAdmin) currentRoleFilter = 2;
                else currentRoleFilter = -1;
            }
            applyFilters();
        });

        btnSortName.setOnClickListener(v -> {
            currentSort = SortOption.NAME;
            updateSortButtons();
            applyFilters();
        });

        btnSortRole.setOnClickListener(v -> {
            currentSort = SortOption.ROLE_LEVEL;
            updateSortButtons();
            applyFilters();
        });

        btnSortNewest.setOnClickListener(v -> {
            currentSort = SortOption.NEWEST;
            updateSortButtons();
            applyFilters();
        });

        updateSortButtons();
        loadUsers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listsRef != null && expiredListener != null) {
            listsRef.removeEventListener(expiredListener);
        }
        if (usersRef != null && usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
        activeDialog = null;
    }

    private void updateSortButtons() {
        btnSortName.setTextColor(currentSort == SortOption.NAME
                ? requireContext().getColor(R.color.colorPrimary)
                : requireContext().getColor(R.color.colorTextSecondary));
        btnSortRole.setTextColor(currentSort == SortOption.ROLE_LEVEL
                ? requireContext().getColor(R.color.colorPrimary)
                : requireContext().getColor(R.color.colorTextSecondary));
        btnSortNewest.setTextColor(currentSort == SortOption.NEWEST
                ? requireContext().getColor(R.color.colorPrimary)
                : requireContext().getColor(R.color.colorTextSecondary));
    }

    private void loadUsers() {
        loadingState.setVisibility(View.VISIBLE);

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot child, @Nullable String previousChildName) {
                String safeEmail = child.getKey();
                String username = child.child("username").getValue(String.class);
                if (username == null || safeEmail == null) return;

                UserManagementAdapter.UserListItem item = new UserManagementAdapter.UserListItem();
                item.safeEmail = safeEmail;
                item.username = username;
                item.email = child.child("email").getValue(String.class);
                item.phone = child.child("phone").getValue(String.class);
                item.bio = child.child("bio").getValue(String.class);
                item.profileImageUrl = child.child("profileImageUrl").getValue(String.class);
                item.role = child.child("role").getValue(String.class);

                Integer rl = child.child("roleLevel").getValue(Integer.class);
                item.roleLevel = rl != null ? rl : 0;

                Boolean on = child.child("online").getValue(Boolean.class);
                item.online = on != null && on;

                Long ls = child.child("lastSeen").getValue(Long.class);
                item.lastSeen = ls != null ? ls : 0L;

                Long ca = child.child("createdAt").getValue(Long.class);
                item.createdAt = ca != null ? ca : 0L;

                allUsers.add(item);

                if (loadingState.getVisibility() == View.VISIBLE) {
                    loadingState.setVisibility(View.GONE);
                }
                applyFilters();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot child, @Nullable String previousChildName) {
                String safeEmail = child.getKey();
                if (safeEmail == null) return;

                for (int i = 0; i < allUsers.size(); i++) {
                    if (allUsers.get(i).safeEmail.equals(safeEmail)) {
                        UserManagementAdapter.UserListItem item = allUsers.get(i);
                        item.username = child.child("username").getValue(String.class);
                        item.email = child.child("email").getValue(String.class);
                        item.phone = child.child("phone").getValue(String.class);
                        item.bio = child.child("bio").getValue(String.class);
                        item.profileImageUrl = child.child("profileImageUrl").getValue(String.class);
                        item.role = child.child("role").getValue(String.class);

                        Integer rl = child.child("roleLevel").getValue(Integer.class);
                        item.roleLevel = rl != null ? rl : 0;

                        Boolean on = child.child("online").getValue(Boolean.class);
                        item.online = on != null && on;

                        Long ls = child.child("lastSeen").getValue(Long.class);
                        item.lastSeen = ls != null ? ls : 0L;

                        Long ca = child.child("createdAt").getValue(Long.class);
                        item.createdAt = ca != null ? ca : 0L;

                        applyFilters();
                        return;
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot child) {
                String safeEmail = child.getKey();
                if (safeEmail == null) return;

                allUsers.removeIf(u -> u.safeEmail.equals(safeEmail));
                applyFilters();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingState.setVisibility(View.GONE);
            }
        };
        usersRef.addChildEventListener(usersListener);
    }

    private void applyFilters() {
        filteredUsers.clear();
        for (UserManagementAdapter.UserListItem u : allUsers) {
            if (currentRoleFilter != -1 && u.roleLevel != currentRoleFilter) continue;
            if (!currentQuery.isEmpty() &&
                    !u.username.toLowerCase().contains(currentQuery) &&
                    (u.email == null || !u.email.toLowerCase().contains(currentQuery)))
                continue;
            filteredUsers.add(u);
        }

        switch (currentSort) {
            case NAME:
                filteredUsers.sort((a, b) -> a.username.compareToIgnoreCase(b.username));
                break;
            case ROLE_LEVEL:
                filteredUsers.sort((a, b) -> b.roleLevel - a.roleLevel);
                break;
            case NEWEST:
                filteredUsers.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
                break;
        }

        adapter.notifyDataSetChanged();
        emptyState.setVisibility(filteredUsers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ─── Original pending deletions logic ───

    private void updatePendingButton() {
        int count = pendingItems.size();
        if (count > 0) {
            btnPendingDeletions.setVisibility(View.VISIBLE);
            btnPendingDeletions.setText("Items Pending Deletion (" + count + ")");
        } else {
            btnPendingDeletions.setVisibility(View.GONE);
        }
    }

    private void showPendingDeletionsDialog() {
        Log.d(TAG, "Opening pending deletion dialog, count=" + pendingItems.size());

        if (pendingItems.isEmpty()) {
            Toast.makeText(requireContext(), "No items pending deletion", Toast.LENGTH_SHORT).show();
            return;
        }

        dialogContainer = new LinearLayout(requireContext());
        dialogContainer.setOrientation(LinearLayout.VERTICAL);
        dialogContainer.setPadding(24, 16, 24, 16);

        buildDialogRows();

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.addView(dialogContainer);

        activeDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Items Pending Deletion")
                .setView(scrollView)
                .setPositiveButton("Close", (dialog, which) -> {
                    if (countdownHandler != null && countdownRunnable != null) {
                        countdownHandler.removeCallbacks(countdownRunnable);
                    }
                    activeDialog = null;
                })
                .show();

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (activeDialog != null && activeDialog.isShowing()) {
                    buildDialogRows();
                    countdownHandler.postDelayed(this, UPDATE_INTERVAL_MS);
                }
            }
        };
        countdownHandler.postDelayed(countdownRunnable, UPDATE_INTERVAL_MS);
    }

    private void buildDialogRows() {
        if (dialogContainer == null) return;
        dialogContainer.removeAllViews();

        long now = System.currentTimeMillis();

        for (PendingItem item : pendingItems) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout textCol = new LinearLayout(requireContext());
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvName = new TextView(requireContext());
            tvName.setText(item.name);
            tvName.setTextSize(16);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);

            long expiredAgo = now - item.timestampExpiresAt;
            long deletingIn = item.deleteAt - now;

            TextView tvExpired = new TextView(requireContext());
            tvExpired.setText("Expired " + formatTimeAgo(Math.max(0, expiredAgo)));
            tvExpired.setTextSize(12);

            TextView tvDeleting = new TextView(requireContext());
            tvDeleting.setText("Deleting in " + formatTimeRemaining(Math.max(0, deletingIn)));
            tvDeleting.setTextSize(12);

            textCol.addView(tvName);
            textCol.addView(tvExpired);
            textCol.addView(tvDeleting);

            Button btnUndo = new Button(requireContext());
            btnUndo.setText("UNDO");
            btnUndo.setOnClickListener(v -> {
                performUndo(item);
                if (activeDialog != null) {
                    activeDialog.dismiss();
                    activeDialog = null;
                }
            });

            row.addView(textCol);
            row.addView(btnUndo);
            dialogContainer.addView(row);
        }
    }

    private void performUndo(PendingItem item) {
        Log.d(TAG, "Undo: pushId=" + item.pushId + ", listType=" + item.listType);

        // Cancel the deletion job
        JobScheduler scheduler = (JobScheduler) requireContext()
                .getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) {
            scheduler.cancel(("del_" + item.pushId).hashCode());
        }

        // Reset expiry flags on the item in the active list
        DatabaseReference itemRef = FirebaseDatabase.getInstance()
                .getReference("lists").child(item.listType).child(item.pushId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("isExpired", false);
        updates.put("deleteAt", -1L);
        itemRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Undo complete: pushId=" + item.pushId);
                    Toast.makeText(requireContext(), item.name + " restored", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Undo failed", e);
                    Toast.makeText(requireContext(), "Undo failed", Toast.LENGTH_SHORT).show();
                });

        // Remove admin notifications for this item
        AdminNotificationHelper.removeNotifications(item.pushId);
    }

    private String formatTimeAgo(long millis) {
        long minutes = millis / 60000;
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        minutes = minutes % 60;
        return hours + "h " + minutes + "m ago";
    }

    private String formatTimeRemaining(long millis) {
        if (millis <= 0) return "any moment";
        long minutes = millis / 60000;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        minutes = minutes % 60;
        return hours + "h " + minutes + "m";
    }

    private static class PendingItem {
        final String pushId;
        final String listType;
        final String name;
        final long deleteAt;
        final long timestampExpiresAt;

        PendingItem(String pushId, String listType, String name, long deleteAt, long timestampExpiresAt) {
            this.pushId = pushId;
            this.listType = listType;
            this.name = name;
            this.deleteAt = deleteAt;
            this.timestampExpiresAt = timestampExpiresAt;
        }
    }
}
