package com.example.mainproj.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.example.mainproj.auth.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PermissionEditorDialog {

    private final Context context;
    private final String gateId;
    private final Runnable onSaved;
    private AlertDialog dialog;

    private com.google.android.material.textfield.TextInputEditText etSearch;
    private RecyclerView recyclerResults;
    private TextView tvNoResults, tvSelectedUser, tvPermLabel;
    private Spinner spinnerPerm;

    private List<SearchResult> results = new ArrayList<>();
    private SearchResultAdapter adapter;
    private SearchResult selectedUser;

    public PermissionEditorDialog(Context context, String gateId, Runnable onSaved) {
        this.context = context;
        this.gateId = gateId;
        this.onSaved = onSaved;
        build();
    }

    private void build() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_gate_permission, null);

        etSearch = view.findViewById(R.id.etSearchUser);
        recyclerResults = view.findViewById(R.id.recyclerSearchResults);
        tvNoResults = view.findViewById(R.id.tvNoResults);
        tvSelectedUser = view.findViewById(R.id.tvSelectedUser);
        tvPermLabel = view.findViewById(R.id.tvPermLabel);
        spinnerPerm = view.findViewById(R.id.spinnerPermissionLevel);

        adapter = new SearchResultAdapter(results, result -> {
            selectedUser = result;
            tvSelectedUser.setText("Selected: " + result.username);
            tvSelectedUser.setVisibility(View.VISIBLE);
            tvPermLabel.setVisibility(View.VISIBLE);
            spinnerPerm.setVisibility(View.VISIBLE);
            recyclerResults.setVisibility(View.GONE);
        });

        recyclerResults.setLayoutManager(new LinearLayoutManager(context));
        recyclerResults.setAdapter(adapter);

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            searchUsers(etSearch.getText().toString().trim());
            return true;
        });

        dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setPositiveButton("Grant Permission", (d, which) -> grantPermission())
                .setNegativeButton("Cancel", null)
                .create();
    }

    public void show() {
        dialog.show();
    }

    private void searchUsers(String query) {
        if (query.isEmpty()) return;

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.orderByChild("username").startAt(query).endAt(query + "\uf8ff")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        results.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String email = child.child("email").getValue(String.class);
                            String username = child.child("username").getValue(String.class);
                            Integer roleLevel = child.child("roleLevel").getValue(Integer.class);
                            if (email != null && username != null) {
                                String safeEmail = email.replace(".", "_");
                                if (safeEmail.equals(SessionManager.getCurrentSafeEmail())) continue;
                                results.add(new SearchResult(safeEmail, username,
                                        roleLevel != null ? roleLevel : 0));
                            }
                        }
                        if (results.isEmpty()) {
                            tvNoResults.setVisibility(View.VISIBLE);
                            recyclerResults.setVisibility(View.GONE);
                        } else {
                            tvNoResults.setVisibility(View.GONE);
                            recyclerResults.setVisibility(View.VISIBLE);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void grantPermission() {
        if (selectedUser == null) {
            Toast.makeText(context, "Please select a user", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!SessionManager.canModifyUser(selectedUser.roleLevel)) {
            Toast.makeText(context, "Cannot modify this user's permissions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Position 0 = View (allowedUsers), Position 1 = Control (allowedAdmins)
        String fieldName = spinnerPerm.getSelectedItemPosition() == 0 ? "allowedUsers" : "allowedAdmins";

        FirebaseDatabase.getInstance()
                .getReference("gates")
                .child(gateId).child(fieldName).child(selectedUser.username)
                .setValue(true)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(context, "Permission granted to " + selectedUser.username, Toast.LENGTH_SHORT).show();
                    if (onSaved != null) onSaved.run();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private static class SearchResult {
        final String safeEmail;
        final String username;
        final int roleLevel;

        SearchResult(String safeEmail, String username, int roleLevel) {
            this.safeEmail = safeEmail;
            this.username = username;
            this.roleLevel = roleLevel;
        }
    }

    private static class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

        private final List<SearchResult> items;
        private final OnResultClickListener listener;

        interface OnResultClickListener {
            void onResultClick(SearchResult result);
        }

        SearchResultAdapter(List<SearchResult> items, OnResultClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SearchResult r = items.get(position);
            holder.text1.setText(r.username);
            holder.text2.setText("Level " + r.roleLevel);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onResultClick(r);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(android.view.View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
