package com.example.mainproj.fragments.subnav;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.example.mainproj.admin.AdminGatePermissionSheet;
import com.example.mainproj.auth.SessionManager;
import com.example.mainproj.gates.Gate;
import com.example.mainproj.gates.GateAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GateControlFragment extends Fragment implements GateAdapter.OnGateClickListener {

    private static final String TAG = "GateControlFragment";
    private static final String PREFS_NAME = "MainProjPrefs";

    private RecyclerView recyclerView;
    private List<Gate> gateList = new ArrayList<>();
    private GateAdapter adapter;
    private DatabaseReference gatesRef;
    private ValueEventListener gatesListener;

    private OkHttpClient client;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sub_gate_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewGates);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new GateAdapter(gateList, this);
        recyclerView.setAdapter(adapter);

        setupOkHttpClient();
        setupFirebase();
        setupTestButton(view);
    }

    private void setupOkHttpClient() {
        client = new OkHttpClient();
    }

    private String getGateControlUrl() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String host = prefs.getString("server_host", "localhost");
        String port = prefs.getString("server_port", "8080");
        return "http://" + host + ":" + port + "/gate/control";
    }

    private void setupFirebase() {
        gatesRef = FirebaseDatabase.getInstance().getReference("gates");

        gatesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                gateList.clear();
                for (DataSnapshot g : snapshot.getChildren()) {
                    String id = g.getKey();
                    String state = g.child("state").getValue(String.class);
                    Integer visLevel = g.child("visibilityLevel").getValue(Integer.class);
                    Integer ctrlLevel = g.child("controlLevel").getValue(Integer.class);
                    Boolean disabledVal = g.child("disabled").getValue(Boolean.class);

                    int vLevel = visLevel != null ? visLevel : 0;
                    int cLevel = ctrlLevel != null ? ctrlLevel : 0;
                    boolean isDisabled = disabledVal != null && disabledVal;

                    if (isDisabled && SessionManager.getRoleLevel() < 2) continue;

                    boolean isExplicitlyAllowed = false;
                    DataSnapshot allowedSnap = g.child("allowedUsers");
                    if (allowedSnap.exists()) {
                        Boolean val = allowedSnap.child(SessionManager.getCurrentUsername()).getValue(Boolean.class);
                        isExplicitlyAllowed = (val != null && val);
                    }

                    if (vLevel == -1) {
                        if (SessionManager.getRoleLevel() < 2 && !isExplicitlyAllowed) continue;
                    } else if (!isExplicitlyAllowed && !SessionManager.canViewGate(vLevel)) {
                        continue;
                    }

                    DataSnapshot adminSnap = g.child("allowedAdmins");

                    Gate gate = new Gate(
                            id,
                            state != null ? state : "closed",
                            state != null ? state : "closed",
                            0L,
                            "",
                            vLevel,
                            cLevel
                    );
                    gate.setDisabled(isDisabled);
                    if (allowedSnap.exists()) {
                        gate.setAllowedUsers((java.util.Map<String, Boolean>) allowedSnap.getValue());
                    }
                    if (adminSnap.exists()) {
                        gate.setAllowedAdmins((java.util.Map<String, Boolean>) adminSnap.getValue());
                    }
                    gateList.add(gate);
                }
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(requireContext(), "Firebase error", Toast.LENGTH_SHORT).show();
                }
            }
        };
        gatesRef.addValueEventListener(gatesListener);
    }

    private void setupTestButton(View view) {
        MaterialButton btnTest = view.findViewById(R.id.sendToServer);
        btnTest.setOnClickListener(v -> testServerConnection());
    }

    private void testServerConnection() {
        TextView statusText = requireView().findViewById(R.id.textview);
        statusText.setText("Testing connection...");
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String host = prefs.getString("server_host", "localhost");
        String port = prefs.getString("server_port", "8080");
        String url = "http://" + host + ":" + port + "/server/control";

        try {
            JSONObject json = new JSONObject();
            json.put("action", "status");

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(json.toString(), MediaType.get("application/json")))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            statusText.setText("Server unreachable");
                            Toast.makeText(requireContext(), "Server unreachable", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (response.isSuccessful()) {
                                statusText.setText("Connected to server");
                                Toast.makeText(requireContext(), "Server connected!", Toast.LENGTH_SHORT).show();
                            } else {
                                statusText.setText("Server error: " + response.code());
                                Toast.makeText(requireContext(), "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    response.close();
                }
            });
        } catch (Exception e) {
            statusText.setText("Connection error");
            Toast.makeText(requireContext(), "Connection error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGateToggle(Gate gate, int position) {
        if (!gate.isClickable()) return;
        if (!SessionManager.canControlGate(gate.getControlLevel())) {
            Toast.makeText(requireContext(), "You don't have permission to control this gate", Toast.LENGTH_SHORT).show();
            return;
        }

        gate.setClickable(false);
        String targetState = gate.isOpen() ? "closed" : "open";
        gate.setActionStatus(targetState.equals("open") ? "Opening..." : "Closing...");
        adapter.updateGate(position, gate);

        sendCommand(gate.getGateNumber(), targetState, position);
    }

    @Override
    public void onGateAdminSettings(Gate gate, int position) {
        AdminGatePermissionSheet sheet = AdminGatePermissionSheet.newInstance(
                gate.getGateNumber(),
                gate.getVisibilityLevel(),
                gate.getControlLevel(),
                gate.isDisabled()
        );
        sheet.show(requireActivity().getSupportFragmentManager(), AdminGatePermissionSheet.TAG);
    }

    private void sendCommand(String gateId, String targetState, int position) {
        try {
            JSONObject json = new JSONObject();
            json.put("gate", gateId);
            json.put("state", targetState);
            json.put("commandId", UUID.randomUUID().toString());
            json.put("userName", SessionManager.getCurrentUsername());

            Request request = new Request.Builder()
                    .url(getGateControlUrl())
                    .post(RequestBody.create(json.toString(), MediaType.get("application/json")))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Gate g = gateList.get(position);
                            g.setActionStatus(g.getState());
                            g.setClickable(true);
                            adapter.updateGate(position, g);
                            Toast.makeText(requireContext(), "Server unreachable", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    response.close();
                }
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Command error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (gatesRef != null && gatesListener != null) {
            gatesRef.removeEventListener(gatesListener);
        }
    }
}