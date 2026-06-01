package com.example.mainproj.fragments.subnav;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mainproj.R;
import com.example.mainproj.messaging.DeletionReceiver;
import com.example.mainproj.messaging.NotificationHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountMgmtFragment extends Fragment {

    private static final String TAG = "AccountMgmtFragment";
    private static final long UPDATE_INTERVAL_MS = 30000;

    private Button btnPendingDeletions;
    private DatabaseReference scheduledRef;
    private ValueEventListener scheduledListener;
    private List<PendingItem> pendingItems = new ArrayList<>();
    private Handler countdownHandler = new Handler(Looper.getMainLooper());
    private AlertDialog activeDialog;
    private LinearLayout dialogContainer;
    private Runnable countdownRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sub_account_mgmt, container, false);

        btnPendingDeletions = view.findViewById(R.id.btnPendingDeletions);
        btnPendingDeletions.setOnClickListener(v -> showPendingDeletionsDialog());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scheduledRef = FirebaseDatabase.getInstance().getReference("scheduledForDeletion");
        scheduledListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingItems.clear();
                long now = System.currentTimeMillis();
                for (DataSnapshot listTypeSnap : snapshot.getChildren()) {
                    String listType = listTypeSnap.getKey();
                    for (DataSnapshot itemSnap : listTypeSnap.getChildren()) {
                        String pushId = itemSnap.getKey();
                        String name = itemSnap.child("name").getValue(String.class);
                        Long deleteAt = itemSnap.child("deleteAt").getValue(Long.class);
                        Long expiresAt = itemSnap.child("timestampExpiresAt").getValue(Long.class);
                        if (pushId != null && name != null && deleteAt != null && deleteAt > now) {
                            pendingItems.add(new PendingItem(
                                    pushId, listType, name, deleteAt,
                                    expiresAt != null ? expiresAt : 0L));
                        }
                    }
                }
                updateButton();
                Log.d(TAG, "Pending items updated, count=" + pendingItems.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "Firebase error: " + error.getMessage());
            }
        };
        scheduledRef.addValueEventListener(scheduledListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (scheduledRef != null && scheduledListener != null) {
            scheduledRef.removeEventListener(scheduledListener);
        }
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
        activeDialog = null;
    }

    private void updateButton() {
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
        Log.d(TAG, "Undo: pushId=" + item.pushId + ", listType=" + item.listType + ", moving back to /lists/");

        Intent delIntent = new Intent(requireContext(), DeletionReceiver.class);
        delIntent.putExtra("pushId", item.pushId);
        delIntent.putExtra("listType", item.listType);
        NotificationHelper.cancelAlarm(requireContext(), (item.pushId + "_del").hashCode(), delIntent);

        DatabaseReference scheduledItemRef = FirebaseDatabase.getInstance()
                .getReference("scheduledForDeletion").child(item.listType).child(item.pushId);

        scheduledItemRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() != null) {
                Object raw = task.getResult().getValue();
                if (raw instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = new HashMap<>((Map<String, Object>) raw);
                    data.remove("deleteAt");
                    FirebaseDatabase.getInstance()
                            .getReference("lists").child(item.listType).child(item.pushId)
                            .setValue(data);
                    scheduledItemRef.removeValue();
                    Log.d(TAG, "Undo complete: pushId=" + item.pushId);
                    Toast.makeText(requireContext(), item.name + " restored", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "Undo failed: could not read data for pushId=" + item.pushId);
                Toast.makeText(requireContext(), "Undo failed", Toast.LENGTH_SHORT).show();
            }
        });
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
