package com.example.mainproj.fragments.subnav;

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
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GateControlFragment extends Fragment implements GateAdapter.OnGateClickListener {

    private static final String TAG = "GateControlFragment";

    // TOGGLE SSL: Set to true to enable HTTPS with custom keystore, false for plain HTTP
    private static final boolean ENABLE_SSL = false;

    // Replace with your PC IP when ENABLE_SSL is true
    private static final String SERVER_URL =
            ENABLE_SSL ? "https://localhost:8080/gate/control" : "http://localhost:8080/gate/control";

    private RecyclerView recyclerView;
    private List<Gate> gateList = new ArrayList<>();
    private GateAdapter adapter;
    private DatabaseReference gatesRef;
    private ValueEventListener gatesListener;

    // SSL-only variables (commented out when ENABLE_SSL = false)
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
        if (ENABLE_SSL) {
            // SSL ENABLED - Uncomment and provide keystore to use
            /*
            try {
                InputStream ksStream = getResources().openRawResource(R.raw.mykeystore);
                KeyStore keyStore = KeyStore.getInstance("BKS");
                keyStore.load(ksStream, "killnoob200".toCharArray());

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);

                X509TrustManager trustManager = (X509TrustManager) tmf.getTrustManagers()[0];

                client = new OkHttpClient.Builder()
                        .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                        .hostnameVerifier((h, s) -> true)
                        .build();
            } catch (Exception e) {
                client = new OkHttpClient();
            }
            */
            client = new OkHttpClient();
        } else {
            client = new OkHttpClient();
        }
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
                    gateList.add(new Gate(
                            id,
                            state != null ? state : "closed",
                            state != null ? state : "closed",
                            0L,
                            ""
                    ));
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

        try {
            JSONObject json = new JSONObject();
            json.put("test", true);

            Request request = new Request.Builder()
                    .url(SERVER_URL)
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
                    response.close();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            statusText.setText("Connected to server");
                            Toast.makeText(requireContext(), "Server connected!", Toast.LENGTH_SHORT).show();
                        });
                    }
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

        gate.setClickable(false);
        String targetState = gate.isOpen() ? "closed" : "open";
        gate.setActionStatus(targetState.equals("open") ? "Opening..." : "Closing...");
        adapter.updateGate(position, gate);

        sendCommand(gate.getGateNumber(), targetState, position);
    }

    private void sendCommand(String gateId, String targetState, int position) {
        try {
            JSONObject json = new JSONObject();
            json.put("gate", gateId);
            json.put("state", targetState);
            json.put("commandId", UUID.randomUUID().toString());

            Request request = new Request.Builder()
                    .url(SERVER_URL)
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