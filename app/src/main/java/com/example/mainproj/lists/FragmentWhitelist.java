package com.example.mainproj.lists;

import android.app.AlertDialog;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.example.mainproj.SearchModule.ListFilter;
import com.example.mainproj.SearchModule.ListsSearchViewModel;
import com.example.mainproj.messaging.AdminNotificationHelper;
import com.example.mainproj.messaging.ExpirationReceiver;
import com.example.mainproj.messaging.NotificationHelper;
import com.example.mainproj.shared_lists_adapter_placeholder.ListAdapter;
import com.example.mainproj.shared_lists_adapter_placeholder.PlaceholderContent;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FragmentWhitelist extends FragmentBaseList {

    private ListAdapter adapter;
    private List<PlaceholderContent.PlaceholderItem> fullList = new ArrayList<>();

    private DatabaseReference whitelistRef;
    private ValueEventListener whitelistListener;
    private View loadingState;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list, container, false);

        loadingState = view.findViewById(R.id.loadingState);
        RecyclerView rv = view.findViewById(R.id.recyclerViewList);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ListAdapter(fullList);
        adapter.setOnDeleteClickListener(this::showDeleteConfirmation);
        rv.setAdapter(adapter);

        ListsSearchViewModel vm =
                new ViewModelProvider(requireActivity()).get(ListsSearchViewModel.class);
        vm.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            adapter.update(ListFilter.apply(fullList, query));
        });

        whitelistRef = FirebaseDatabase.getInstance().getReference("lists/whitelist");

        whitelistListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (loadingState != null) loadingState.setVisibility(View.GONE);
                fullList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    PlaceholderContent.PlaceholderItem item = parseItem(child);
                    fullList.add(item);
                }
                fullList.sort((a, b) -> Long.compare(b.timestampWhenAdded, a.timestampWhenAdded));
                adapter.update(fullList);
                refreshFilteredList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (loadingState != null) loadingState.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Firebase error", Toast.LENGTH_SHORT).show();
            }
        };

        whitelistRef.addValueEventListener(whitelistListener);

        View fabAdd = view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> showAddDialog());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (whitelistRef != null && whitelistListener != null) {
            whitelistRef.removeEventListener(whitelistListener);
        }
    }

    private PlaceholderContent.PlaceholderItem parseItem(DataSnapshot child) {
        String id = child.getKey();
        String name = child.child("name").getValue(String.class);
        String byWho = child.child("byWho").getValue(String.class);
        String reason = child.child("reason").getValue(String.class);
        Long timestampWhenAdded = child.child("timestampWhenAdded").getValue(Long.class);
        Long timestampExpiresAt = child.child("timestampExpiresAt").getValue(Long.class);
        Boolean isRemoved = child.child("isRemoved").getValue(Boolean.class);
        Boolean isExpired = child.child("isExpired").getValue(Boolean.class);
        Long deleteAt = child.child("deleteAt").getValue(Long.class);

        return new PlaceholderContent.PlaceholderItem(
                id,
                isRemoved != null && isRemoved,
                timestampWhenAdded != null ? timestampWhenAdded : 0L,
                name != null ? name : "",
                byWho != null ? byWho : "",
                reason != null ? reason : "",
                timestampExpiresAt != null ? timestampExpiresAt : -1L,
                isExpired != null && isExpired,
                deleteAt != null ? deleteAt : -1L
        );
    }

    private void showAddDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_item, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etReason = dialogView.findViewById(R.id.etReason);
        Spinner spinnerExpiry = dialogView.findViewById(R.id.spinnerExpiry);

        String[] expiryOptions = {"Never", "1 Day", "1 Week", "1 Month"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                expiryOptions
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExpiry.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add to Whitelist")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String reason = etReason.getText() != null ? etReason.getText().toString().trim() : "";
                    int selectedPos = spinnerExpiry.getSelectedItemPosition();

                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check if the name is an admin — admins cannot be added to lists
                    DatabaseReference userCheckRef = FirebaseDatabase.getInstance().getReference("users");
                    userCheckRef.orderByChild("username").equalTo(name)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        for (DataSnapshot child : snapshot.getChildren()) {
                                            Integer rl = child.child("roleLevel").getValue(Integer.class);
                                            if (rl != null && rl >= 1) {
                                                Toast.makeText(requireContext(), "Admins cannot be added to the whitelist", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                        }
                                    }
                                    // Not an admin — proceed to add
                                    proceedAddToWhitelist(name, reason, selectedPos);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(requireContext(), "Firebase error", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void proceedAddToWhitelist(String name, String reason, int selectedPos) {
        String byWho = "unknown";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            byWho = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        long now = System.currentTimeMillis();
        long timestampExpiresAt;
        switch (selectedPos) {
            case 1:
                timestampExpiresAt = now + 86400000L;
                break;
            case 2:
                timestampExpiresAt = now + 604800000L;
                break;
            case 3:
                timestampExpiresAt = now + 2592000000L;
                break;
            default:
                timestampExpiresAt = -1L;
        }

        PlaceholderContent.PlaceholderItem item = new PlaceholderContent.PlaceholderItem(
                null,
                false,
                now,
                name,
                byWho,
                reason,
                timestampExpiresAt,
                false,
                -1L
        );

        String newKey = whitelistRef.push().getKey();
        item.id = newKey;
        whitelistRef.child(newKey).setValue(item);

        if (timestampExpiresAt != -1L) {
            Intent expIntent = new Intent(requireContext(), ExpirationReceiver.class);
            expIntent.putExtra("pushId", newKey);
            expIntent.putExtra("listType", "whitelist");
            expIntent.putExtra("itemName", name);
            expIntent.putExtra("byWho", byWho);
            expIntent.putExtra("reason", reason);
            expIntent.putExtra("timestampWhenAdded", now);
            expIntent.putExtra("timestampExpiresAt", timestampExpiresAt);
            NotificationHelper.scheduleAlarm(requireContext(), newKey.hashCode(), timestampExpiresAt, expIntent);
            Log.d("FragmentWhitelist", "Item added with expiry: id=" + newKey + ", expiresAt=" + timestampExpiresAt + ", alarm scheduled");
        }

        Toast.makeText(requireContext(), "Item added", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteConfirmation(int position, PlaceholderContent.PlaceholderItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove " + item.name + "?")
                .setMessage("This will remove the item from the whitelist.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    if (item.isExpired && item.id != null) {
                        JobScheduler scheduler = (JobScheduler) requireContext()
                                .getSystemService(Context.JOB_SCHEDULER_SERVICE);
                        if (scheduler != null) {
                            scheduler.cancel(("del_" + item.id).hashCode());
                        }
                        AdminNotificationHelper.removeNotifications(item.id);
                    }
                    fullList.remove(item);
                    adapter.removeItem(position);
                    if (item.id != null) {
                        whitelistRef.child(item.id).removeValue();
                    } else {
                        Toast.makeText(requireContext(), "Item removed locally", Toast.LENGTH_SHORT).show();
                    }
                    refreshFilteredList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void proceedAddToWhitelist(String name, String reason) {
        String byWho = "unknown";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            byWho = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        long now = System.currentTimeMillis();
        long timestampExpiresAt = -1L;

        PlaceholderContent.PlaceholderItem item = new PlaceholderContent.PlaceholderItem(
                null,
                false,
                now,
                name,
                byWho,
                reason,
                timestampExpiresAt,
                false,
                -1L
        );

        String newKey = whitelistRef.push().getKey();
        item.id = newKey;
        whitelistRef.child(newKey).setValue(item);

        Toast.makeText(requireContext(), "Item added", Toast.LENGTH_SHORT).show();
    }

    private void refreshFilteredList() {
        ListsSearchViewModel vm =
                new ViewModelProvider(requireActivity()).get(ListsSearchViewModel.class);
        String query = vm.getSearchQuery().getValue();
        if (query != null && !query.isEmpty()) {
            adapter.update(ListFilter.apply(fullList, query));
        } else {
            adapter.update(fullList);
        }
    }

    @Override
    public List<PlaceholderContent.PlaceholderItem> getFullList() {
        return fullList;
    }

    @Override
    public void updateList(List<PlaceholderContent.PlaceholderItem> filtered) {
        adapter.update(filtered);
    }
}
