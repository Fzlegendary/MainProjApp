package com.example.mainproj;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.adapters.GateLogAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GateLogActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GateLogAdapter adapter;
    private List<GateLogAdapter.GateLogEntry> entries = new ArrayList<>();
    private ProgressBar loadingState;
    private TextView tvEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gate_log);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerViewLogs);
        loadingState = findViewById(R.id.loadingState);
        tvEmpty = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GateLogAdapter(entries);
        recyclerView.setAdapter(adapter);

        loadLogs();
    }

    private void loadLogs() {
        loadingState.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference("logs");
        logsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                entries.clear();
                for (DataSnapshot dateSnap : snapshot.getChildren()) {
                    String dateKey = dateSnap.getKey();
                    for (DataSnapshot hourSnap : dateSnap.getChildren()) {
                        String hourKey = hourSnap.getKey();
                        for (DataSnapshot entrySnap : hourSnap.getChildren()) {
                            String secondKey = entrySnap.getKey();
                            String gate = entrySnap.child("gate").getValue(String.class);
                            String ip = entrySnap.child("ip").getValue(String.class);
                            String user = entrySnap.child("user").getValue(String.class);
                            Long timestamp = entrySnap.child("timestamp").getValue(Long.class);
                            String fromTo = entrySnap.child("from_to").getValue(String.class);
                            String commandId = entrySnap.child("commandId").getValue(String.class);
                            String action = entrySnap.child("action").getValue(String.class);

                            if (gate == null || timestamp == null) continue;

                            entries.add(new GateLogAdapter.GateLogEntry(
                                    gate, ip, user, timestamp, fromTo, commandId, action,
                                    dateKey != null ? dateKey : "",
                                    (hourKey != null ? hourKey : "") + ":" + (secondKey != null ? secondKey : "")
                            ));
                        }
                    }
                }

                Collections.sort(entries, (a, b) -> Long.compare(b.timestamp, a.timestamp));

                loadingState.setVisibility(View.GONE);
                if (entries.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingState.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Failed to load logs");
            }
        });
    }
}
