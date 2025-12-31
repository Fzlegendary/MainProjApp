package com.example.mainproj;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class HomeActivity extends AppCompatActivity
        implements GateAdapter.OnGateClickListener {

    private static final String TAG = "HomeActivity";
    private static final String SERVER_URL = "https://127.0.0.1:8080/gate/control";

    private OkHttpClient client;
    private final List<Gate> gateList = new ArrayList<>();
    private GateAdapter adapter;
    private DatabaseReference gatesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewGates);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GateAdapter(gateList, this);
        recyclerView.setAdapter(adapter);

        FirebaseApp.initializeApp(this);
        gatesRef = FirebaseDatabase.getInstance().getReference("gates");

        setupHttpsClient();
        loadGatesFromFirebase();
    }

    /**
     * HTTPS client using BKS truststore from res/raw
     */
    private void setupHttpsClient() {
        try {
            InputStream ksStream = getResources().openRawResource(R.raw.mykeystore);
            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(ksStream, "killnoob200".toCharArray());

            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            X509TrustManager trustManager =
                    (X509TrustManager) tmf.getTrustManagers()[0];

            client = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .build();

            Log.d(TAG, "HTTPS client initialized");

        } catch (Exception e) {
            Log.e(TAG, "SSL setup failed – using default client", e);
            client = new OkHttpClient();
        }
    }

    /**
     * Firebase loader
     */
    private void loadGatesFromFirebase() {
        gatesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                gateList.clear();

                for (DataSnapshot gateSnap : snapshot.getChildren()) {
                    String id = gateSnap.getKey();
                    String state = gateSnap.child("state").getValue(String.class);
                    Long ack = gateSnap.child("lastAckAt").getValue(Long.class);
                    String cmd = gateSnap.child("lastCommandId").getValue(String.class);

                    gateList.add(new Gate(
                            id,
                            state != null ? state : "closed",
                            state != null ? state : "closed",
                            ack != null ? ack : 0L,
                            cmd != null ? cmd : ""
                    ));
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this,
                        "Firebase load failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * UI click handler
     */
    @Override
    public void onGateToggle(Gate gate, int position) {
        if (!gate.isClickable()) return;

        gate.setClickable(false);

        boolean opening = !gate.isOpen();
        String targetState = opening ? "open" : "closed";

        gate.setActionStatus(opening ? "Opening..." : "Closing...");
        adapter.updateGate(position, gate);

        sendCommand(gate, position, targetState);
    }

    /**
     * Network command
     */
    private void sendCommand(Gate gate, int position, String targetState) {
        String commandId = UUID.randomUUID().toString();

        try {
            JSONObject json = new JSONObject();
            json.put("gate", gate.getGateNumber());
            json.put("state", targetState);
            json.put("commandId", commandId);

            Request request = new Request.Builder()
                    .url(SERVER_URL)
                    .post(RequestBody.create(
                            json.toString(),
                            MediaType.get("application/json")
                    ))
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Request failed", e);
                    runOnUiThread(() -> rollback(position));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try {
                        String body = response.body() != null
                                ? response.body().string()
                                : "";
                        response.close();

                        JSONObject resp = new JSONObject(body);
                        String status = resp.optString("status", "ok");

                        runOnUiThread(() -> {
                            if ("rejected".equals(status)) {
                                rollback(position);
                                return;
                            }

                            gate.setState(targetState);
                            gate.setActionStatus(targetState);
                            gate.setClickable(true);
                            adapter.updateGate(position, gate);

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("state", gate.getState());
                            updates.put("lastAckAt", System.currentTimeMillis());
                            updates.put("lastCommandId", commandId);

                            gatesRef.child(gate.getGateNumber())
                                    .updateChildren(updates);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Response error", e);
                        runOnUiThread(() -> rollback(position));
                    }
                }
            });

        } catch (Exception e) {
            rollback(position);
        }
    }

    /**
     * Rollback UI
     */
    private void rollback(int position) {
        Gate g = gateList.get(position);
        g.setState(g.isOpen() ? "open" : "closed");
        g.setActionStatus(g.getState());
        g.setClickable(true);
        adapter.updateGate(position, g);

        Toast.makeText(this,
                "Server unreachable, try again",
                Toast.LENGTH_SHORT).show();
    }
}
