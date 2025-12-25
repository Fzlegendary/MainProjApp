package com.example.mainproj;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeActivity extends AppCompatActivity implements GateAdapter.OnGateClickListener {

    private final OkHttpClient client = new OkHttpClient();
    private final List<Gate> gateList = new ArrayList<>();
    private GateAdapter adapter;
    private DatabaseReference gatesRef;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewGates);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize 4 gates
        for (int i = 1; i <= 4; i++) {
            gateList.add(new Gate("Gate " + i, "closed", "Ready"));
        }

        adapter = new GateAdapter(gateList, this);
        recyclerView.setAdapter(adapter);

        // Correct Europe-west1 Firebase URL
        gatesRef = FirebaseDatabase.getInstance(
                "https://mainproj-5e50a-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("gates");

        // Force reconnect
        FirebaseDatabase.getInstance().goOffline();
        FirebaseDatabase.getInstance(
                "https://mainproj-5e50a-default-rtdb.europe-west1.firebasedatabase.app"
        ).goOnline();

        ensureFirebaseNodes();
    }

    /** Ensure Firebase has initial data */
    private void ensureFirebaseNodes() {
        gatesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (int i = 0; i < gateList.size(); i++) {
                    String key = "gate" + (i + 1);
                    if (!task.getResult().hasChild(key)) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("state", "closed");
                        data.put("actionStatus", "Ready");
                        gatesRef.child(key).setValue(data);
                    }
                }
                attachFirebaseListeners();
            } else {
                Log.e("FirebaseInit", "Failed to get initial data: " + task.getException());
            }
        });
    }

    /** Attach optimized Firebase listeners using ChildEventListener */
    private void attachFirebaseListeners() {
        for (int i = 0; i < gateList.size(); i++) {
            int index = i;
            String key = "gate" + (i + 1);

            gatesRef.child(key).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) { }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                    Gate gate = gateList.get(index);
                    String childKey = snapshot.getKey();
                    String value = snapshot.getValue(String.class);
                    boolean changed = false;

                    if (childKey == null || value == null) return;

                    if (childKey.equals("state") && !value.equalsIgnoreCase(gate.getState())) {
                        gate.setState(value);
                        changed = true;
                    }
                    if (childKey.equals("actionStatus") && !value.equals(gate.getActionStatus())) {
                        gate.setActionStatus(value);
                        changed = true;
                    }

                    if (changed) {
                        runOnUiThread(() -> adapter.updateGate(index, gate));
                        Log.d("FirebaseListener", "Gate " + gate.getGateNumber() +
                                " updated: state=" + gate.getState() +
                                " actionStatus=" + gate.getActionStatus());
                    }
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) { }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) { }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("FirebaseListener", "Error: " + error.getMessage());
                }
            });
        }
    }

    /** Handle gate button click */
    @Override
    public void onGateToggle(Gate gate, int position) {
        boolean opening = !gate.isOpen();
        String optimisticStatus = opening ? "Opening..." : "Closing...";
        String targetState = opening ? "open" : "closed";

        // Immediate UI feedback
        gate.setActionStatus(optimisticStatus);
        adapter.updateGate(position, gate);

        sendCommand(gate.getGateNumber(), targetState);
        Log.d("GateToggle", "User clicked gate " + gate.getGateNumber() +
                ", optimisticStatus=" + optimisticStatus);
    }

    /** Send command to server */
    private void sendCommand(String gateNumber, String state) {
        String json = "{\"gate\":\"" + gateNumber + "\",\"state\":\"" + state + "\"}";

        Request request = new Request.Builder()
                .url("http://127.0.0.1:8080/gate/control")
                .post(RequestBody.create(json, MediaType.get("application/json; charset=utf-8")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(HomeActivity.this,
                        "Server unreachable", Toast.LENGTH_SHORT).show());
                Log.e("ServerCall", "Failure: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(HomeActivity.this,
                            "Command rejected", Toast.LENGTH_SHORT).show());
                    Log.e("ServerCall", "Rejected: " + response.code());
                } else {
                    Log.d("ServerCall", "Command accepted: " + gateNumber + " -> " + state);
                }
            }
        });
    }
}
