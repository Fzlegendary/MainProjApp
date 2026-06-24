package com.example.mainproj.fragments.subnav;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.example.mainproj.adapters.UserManagementAdapter;
import com.example.mainproj.auth.SessionManager;
import com.example.mainproj.userSystem.UserProfileActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class AccountMgmtFragment extends Fragment {

    private static final String TAG = "AccountMgmtFragment";

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
        // Admin guard
        if (!SessionManager.isAdmin()) {
            accessDenied.setVisibility(View.VISIBLE);
            contentSection.setVisibility(View.GONE);
            return;
        }

        contentSection.setVisibility(View.VISIBLE);

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
        if (usersRef != null && usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
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

}
