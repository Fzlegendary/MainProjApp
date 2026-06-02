package com.example.mainproj.userSystem;

import android.app.Dialog;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.example.mainproj.auth.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserGatePermissionSheet extends BottomSheetDialogFragment {

    public static final String TAG = "UserGatePermissionSheet";

    private static final String ARG_USERNAME = "target_username";

    private static final int MODE_USERS = 0;
    private static final int MODE_ADMINS = 1;

    private String targetUsername;
    private int currentMode = MODE_USERS;

    private TextView tvTitle, tvSubtitle;
    private TabLayout tabLayout;
    private RecyclerView recyclerGates;
    private MaterialButton btnSave;

    private List<GateCheckEntry> gateList = new ArrayList<>();
    private GateCheckAdapter adapter;

    public static UserGatePermissionSheet newInstance(String targetUsername) {
        UserGatePermissionSheet sheet = new UserGatePermissionSheet();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, targetUsername);
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
        return inflater.inflate(R.layout.bottom_sheet_user_gate_permissions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            targetUsername = args.getString(ARG_USERNAME);
        }

        tvTitle = view.findViewById(R.id.bsTitle);
        tvSubtitle = view.findViewById(R.id.bsSubtitle);
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerGates = view.findViewById(R.id.recyclerGates);
        btnSave = view.findViewById(R.id.btnSave);

        tvTitle.setText("Gate Permissions");
        tvSubtitle.setText(targetUsername != null ? "Select gates for " + targetUsername : "");

        // Show TabLayout only for super admins
        if (SessionManager.isSuperAdmin()) {
            tabLayout.setVisibility(View.VISIBLE);
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    currentMode = tab.getPosition() == 0 ? MODE_USERS : MODE_ADMINS;
                    loadGates();
                }
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}
                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }

        adapter = new GateCheckAdapter(gateList);
        recyclerGates.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerGates.setAdapter(adapter);

        btnSave.setOnClickListener(v -> savePermissions());

        loadGates();
    }

    private void loadGates() {
        if (targetUsername == null || targetUsername.isEmpty()) return;

        String fieldName = currentMode == MODE_USERS ? "allowedUsers" : "allowedAdmins";
        String currentAdmin = SessionManager.getCurrentUsername();
        boolean isSuper = SessionManager.isSuperAdmin();

        DatabaseReference gatesRef = FirebaseDatabase.getInstance().getReference("gates");
        gatesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                gateList.clear();
                for (DataSnapshot g : snapshot.getChildren()) {
                    String gateId = g.getKey();
                    if (gateId == null) continue;

                    // Regular admins in MODE_USERS: only see gates where they're in allowedAdmins
                    if (!isSuper && currentMode == MODE_USERS) {
                        DataSnapshot adminSnap = g.child("allowedAdmins");
                        Boolean isAdminForGate = adminSnap.child(currentAdmin).getValue(Boolean.class);
                        if (isAdminForGate == null || !isAdminForGate) continue;
                    }

                    String state = g.child("state").getValue(String.class);
                    boolean disabled = Boolean.TRUE.equals(g.child("disabled").getValue(Boolean.class));

                    boolean isChecked = false;
                    DataSnapshot fieldSnap = g.child(fieldName);
                    if (fieldSnap.exists()) {
                        Boolean val = fieldSnap.child(targetUsername).getValue(Boolean.class);
                        isChecked = (val != null && val);
                    }

                    gateList.add(new GateCheckEntry(gateId, state != null ? state : "closed", disabled, isChecked));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load gates", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePermissions() {
        if (targetUsername == null || targetUsername.isEmpty()) return;

        String fieldName = currentMode == MODE_USERS ? "allowedUsers" : "allowedAdmins";

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        DatabaseReference gatesRef = FirebaseDatabase.getInstance().getReference("gates");
        Map<String, Object> updates = new HashMap<>();

        for (GateCheckEntry entry : gateList) {
            String path = entry.gateId + "/" + fieldName + "/" + targetUsername;
            if (entry.isChecked) {
                updates.put(path, true);
            } else {
                updates.put(path, null);
            }
        }

        gatesRef.updateChildren(updates).addOnCompleteListener(task -> {
            btnSave.setEnabled(true);
            btnSave.setText("Save");
            if (task.isSuccessful()) {
                Toast.makeText(requireContext(), "Permissions saved", Toast.LENGTH_SHORT).show();
                dismiss();
            } else {
                Toast.makeText(requireContext(), "Failed to save permissions", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class GateCheckEntry {
        final String gateId;
        final String state;
        final boolean disabled;
        boolean isChecked;

        GateCheckEntry(String gateId, String state, boolean disabled, boolean isChecked) {
            this.gateId = gateId;
            this.state = state;
            this.disabled = disabled;
            this.isChecked = isChecked;
        }
    }

    private static class GateCheckAdapter extends RecyclerView.Adapter<GateCheckAdapter.ViewHolder> {

        private final List<GateCheckEntry> items;

        GateCheckAdapter(List<GateCheckEntry> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GateCheckEntry entry = items.get(position);
            String[] parts = entry.gateId.split("e");
            String displayName = parts.length >= 2 ? parts[0] + "e " + parts[1] : entry.gateId;
            holder.checkedText.setText(displayName + " (" + entry.state + ")");
            holder.checkedText.setChecked(entry.isChecked);
            if (entry.disabled) {
                holder.itemView.setAlpha(0.4f);
            } else {
                holder.itemView.setAlpha(1f);
            }
            holder.itemView.setOnClickListener(v -> {
                if (entry.disabled) return;
                entry.isChecked = !entry.isChecked;
                holder.checkedText.setChecked(entry.isChecked);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            CheckedTextView checkedText;

            ViewHolder(View v) {
                super(v);
                checkedText = v.findViewById(android.R.id.text1);
            }
        }
    }
}
