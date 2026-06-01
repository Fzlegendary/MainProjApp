package com.example.mainproj.admin;

import android.app.Dialog;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.example.mainproj.auth.SessionManager;
import com.example.mainproj.messaging.UserProfileBottomSheet;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminGatePermissionSheet extends BottomSheetDialogFragment {

    public static final String TAG = "AdminGatePermissionSheet";

    private static final String ARG_GATE_ID = "gate_id";
    private static final String ARG_VIS_LEVEL = "vis_level";
    private static final String ARG_CTRL_LEVEL = "ctrl_level";
    private static final String ARG_DISABLED = "disabled";

    private String gateId;
    private int currentVisLevel;
    private int currentCtrlLevel;
    private boolean currentDisabled;

    private TextView tvTitle, tvSubtitle;
    private View gateLevelControls, controlLevelRow, disableControlRow, visibilityLevelRow;
    private Spinner spinnerVis, spinnerCtrl;
    private SwitchCompat switchDisable;
    private MaterialButton btnSaveLevels;
    private RecyclerView recyclerUsers;
    private MaterialButton btnAddUser;

    private DatabaseReference userRef;
    private ValueEventListener userListener;

    private List<UserPermissionEntry> userList = new ArrayList<>();
    private UserPermissionAdapter adapter;

    public static AdminGatePermissionSheet newInstance(String gateId, int visLevel, int ctrlLevel, boolean disabled) {
        AdminGatePermissionSheet sheet = new AdminGatePermissionSheet();
        Bundle args = new Bundle();
        args.putString(ARG_GATE_ID, gateId);
        args.putInt(ARG_VIS_LEVEL, visLevel);
        args.putInt(ARG_CTRL_LEVEL, ctrlLevel);
        args.putBoolean(ARG_DISABLED, disabled);
        sheet.setArguments(args);
        return sheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int peekHeight = (int) (metrics.heightPixels * 0.75);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight(peekHeight);
                behavior.setMaxHeight(metrics.heightPixels);
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                behavior.setHideable(true);
                behavior.setSkipCollapsed(false);
            }
        });

        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_gate_permissions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            gateId = args.getString(ARG_GATE_ID);
            currentVisLevel = args.getInt(ARG_VIS_LEVEL);
            currentCtrlLevel = args.getInt(ARG_CTRL_LEVEL);
            currentDisabled = args.getBoolean(ARG_DISABLED, false);
        }

        tvTitle = view.findViewById(R.id.bsGateTitle);
        tvSubtitle = view.findViewById(R.id.bsGateSubtitle);
        gateLevelControls = view.findViewById(R.id.gateLevelControls);
        visibilityLevelRow = view.findViewById(R.id.visibilityLevelRow);
        controlLevelRow = view.findViewById(R.id.controlLevelRow);
        disableControlRow = view.findViewById(R.id.disableControlRow);
        spinnerVis = view.findViewById(R.id.spinnerVisibilityLevel);
        spinnerCtrl = view.findViewById(R.id.spinnerControlLevel);
        switchDisable = view.findViewById(R.id.switchDisableGate);
        btnSaveLevels = view.findViewById(R.id.btnSaveLevels);
        recyclerUsers = view.findViewById(R.id.recyclerUsers);
        btnAddUser = view.findViewById(R.id.btnAddUser);

        if (gateId != null) {
            tvTitle.setText(gateId + " - Permissions");
            tvSubtitle.setText("Visibility: Lv" + currentVisLevel + " | Control: Lv" + currentCtrlLevel);
        }

        updateUIVisibility();

        adapter = new UserPermissionAdapter(userList, SessionManager.getRoleLevel() >= 2 ? user -> {
            UserProfileBottomSheet sheet = UserProfileBottomSheet.newInstance(
                    user.username, "", "", false, user.safeEmail, user.roleLevel
            );
            sheet.setActionListener(new UserProfileBottomSheet.ProfileActionListener() {
                @Override
                public void onSendMessage(String username) {}
                @Override
                public void onViewProfile(String username) {}
                @Override
                public void onBlockUser(String username) {}
                @Override
                public void onManageRole(String username, String safeEmail) {
                    showRoleChangeDialog(username, safeEmail, user.roleLevel);
                }
                @Override
                public void onManageGatePermissions(String username, String safeEmail) {}
            });
            sheet.show(requireActivity().getSupportFragmentManager(), UserProfileBottomSheet.TAG);
        } : null);

        recyclerUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerUsers.setAdapter(adapter);

        btnSaveLevels.setOnClickListener(v -> saveGateLevels());

        btnAddUser.setOnClickListener(v -> {
            PermissionEditorDialog dialog = new PermissionEditorDialog(requireContext(), gateId, () -> loadUsers());
            dialog.show();
        });

        loadUsers();

        String safeEmail = SessionManager.getCurrentSafeEmail();
        if (!safeEmail.isEmpty()) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(safeEmail).child("roleLevel");
            userListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer newLevel = snapshot.getValue(Integer.class);
                    if (newLevel != null && newLevel != SessionManager.getRoleLevel()) {
                        SessionManager.setRoleLevel(newLevel);
                        updateUIVisibility();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            userRef.addValueEventListener(userListener);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
    }

    private void updateUIVisibility() {
        int roleLevel = SessionManager.getRoleLevel();

        if (currentDisabled && roleLevel < 2) {
            gateLevelControls.setVisibility(View.GONE);
            btnAddUser.setVisibility(View.GONE);

        } else if (roleLevel == 2) {
            gateLevelControls.setVisibility(View.VISIBLE);

            setupSpinnerAdapter(spinnerVis, 2);
            spinnerVis.setSelection(currentVisLevel);

            setupSpinnerAdapter(spinnerCtrl, 2);
            spinnerCtrl.setSelection(currentCtrlLevel);
            disableControlRow.setVisibility(View.VISIBLE);
            switchDisable.setChecked(currentDisabled);

            btnAddUser.setVisibility(View.VISIBLE);

        } else if (roleLevel == 1) {
            gateLevelControls.setVisibility(View.VISIBLE);

            if (currentCtrlLevel < 1) {
                setupSpinnerAdapter(spinnerVis, 1);
                spinnerVis.setSelection(Math.min(currentVisLevel, 1));
            }
            else {
                visibilityLevelRow.setVisibility(View.GONE);
            }
            controlLevelRow.setVisibility(View.GONE);
            disableControlRow.setVisibility(View.GONE);
            btnAddUser.setVisibility(View.GONE);
        }
    }

    private void setupSpinnerAdapter(Spinner spinner, int maxLevel) {
        String[] allOptions = {"Level 0 (All Users)", "Level 1 (Admin)", "Level 2 (Super Admin)"};
        if (maxLevel >= 2) {
            spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, allOptions));
        } else if (maxLevel == 1) {
            spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item,
                    new String[]{allOptions[0], allOptions[1]}));
        }
    }

    private void loadUsers() {
        DatabaseReference permsRef = FirebaseDatabase.getInstance()
                .getReference("gate-permissions").child(gateId).child("users");

        permsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String safeEmail = userSnap.getKey();
                    Integer grantedLevel = userSnap.child("grantedLevel").getValue(Integer.class);
                    if (safeEmail == null || grantedLevel == null) continue;
                    lookupUserInfo(safeEmail, grantedLevel);
                }
                if (!snapshot.hasChildren()) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void lookupUserInfo(String safeEmail, int grantedLevel) {
        FirebaseDatabase.getInstance().getReference("users").child(safeEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String username = snapshot.child("username").getValue(String.class);
                        Integer roleLevel = snapshot.child("roleLevel").getValue(Integer.class);
                        if (username == null) username = safeEmail;
                        userList.add(new UserPermissionEntry(
                                safeEmail, username,
                                roleLevel != null ? roleLevel : 0,
                                grantedLevel
                        ));
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        userList.add(new UserPermissionEntry(safeEmail, safeEmail, 0, grantedLevel));
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void saveGateLevels() {
        int newVis = spinnerVis.getSelectedItemPosition();
        int newCtrl = spinnerCtrl.getSelectedItemPosition();
        boolean newDisabled = switchDisable.getVisibility() == View.VISIBLE && switchDisable.isChecked();

        SessionManager.setGateLevels(gateId, newVis, newCtrl);
        SessionManager.setGateDisabled(gateId, newDisabled);
        currentVisLevel = newVis;
        currentCtrlLevel = newCtrl;
        currentDisabled = newDisabled;
        tvSubtitle.setText("Visibility: Lv" + newVis + " | Control: Lv" + newCtrl);
        Toast.makeText(requireContext(), "Gate levels saved", Toast.LENGTH_SHORT).show();
    }

    private void showRoleChangeDialog(String username, String safeEmail, int targetRoleLevel) {
        if (!SessionManager.canModifyUser(targetRoleLevel)) {
            Toast.makeText(requireContext(), "Cannot modify this user's role", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options;
        if (SessionManager.isSuperAdmin()) {
            options = new String[]{"User", "Admin", "Super Admin"};
        } else {
            options = new String[]{"User", "Admin"};
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Change Role - " + username)
                .setItems(options, (dialog, which) -> {
                    String newRole;
                    int newLevel;
                    switch (which) {
                        case 0: newRole = "user"; newLevel = 0; break;
                        case 1: newRole = "admin"; newLevel = 1; break;
                        case 2: newRole = "superadmin"; newLevel = 2; break;
                        default: return;
                    }
                    SessionManager.setUserRole(safeEmail, newRole, newLevel);
                    Toast.makeText(requireContext(), "Role updated to " + newRole, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static class UserPermissionEntry {
        public final String safeEmail;
        public final String username;
        public final int roleLevel;
        public final int grantedLevel;

        public UserPermissionEntry(String safeEmail, String username, int roleLevel, int grantedLevel) {
            this.safeEmail = safeEmail;
            this.username = username;
            this.roleLevel = roleLevel;
            this.grantedLevel = grantedLevel;
        }
    }

    private static class UserPermissionAdapter extends RecyclerView.Adapter<UserPermissionAdapter.ViewHolder> {

        private final List<UserPermissionEntry> items;
        private final OnUserClickListener listener;

        interface OnUserClickListener {
            void onUserClick(UserPermissionEntry user);
        }

        UserPermissionAdapter(List<UserPermissionEntry> items, @Nullable OnUserClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserPermissionEntry entry = items.get(position);
            holder.text1.setText(entry.username);
            String permStr = entry.grantedLevel == 2 ? "Control" : "View";
            holder.text2.setText("Role: Lv" + entry.roleLevel + " | Permission: " + permStr);
            if (listener != null) {
                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onUserClick(entry);
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
