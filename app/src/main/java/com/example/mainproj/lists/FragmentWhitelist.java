package com.example.mainproj.lists;

import android.app.AlertDialog;
import android.os.Bundle;
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
    private static final String LIST_TYPE = "whitelist";

    private DatabaseReference whitelistRef;
    private ValueEventListener whitelistListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list, container, false);

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

        return new PlaceholderContent.PlaceholderItem(
                id,
                isRemoved != null && isRemoved,
                timestampWhenAdded != null ? timestampWhenAdded : 0L,
                name != null ? name : "",
                byWho != null ? byWho : "",
                reason != null ? reason : "",
                timestampExpiresAt != null ? timestampExpiresAt : -1L
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

                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String byWho = "unknown";
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        byWho = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    }

                    long now = System.currentTimeMillis();
                    int selectedPos = spinnerExpiry.getSelectedItemPosition();
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
                            timestampExpiresAt
                    );

                    String newKey = whitelistRef.push().getKey();
                    item.id = newKey;
                    whitelistRef.child(newKey).setValue(item);

                    Toast.makeText(requireContext(), "Item added", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmation(int position, PlaceholderContent.PlaceholderItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove " + item.name + "?")
                .setMessage("This will remove the item from the whitelist.")
                .setPositiveButton("Remove", (dialog, which) -> {
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
