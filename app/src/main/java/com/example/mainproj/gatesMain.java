package com.example.mainproj;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.auth.SessionManager;
import com.example.mainproj.gates.Gate;
import com.example.mainproj.gates.GateAdapter;
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

public class gatesMain extends BaseActivity
        implements GateAdapter.OnGateClickListener {

    private static final String TAG = "gatesMain";
    private static final String PREFS_NAME = "MainProjPrefs";

    private OkHttpClient client;
    private final List<Gate> gateList = new ArrayList<>();
    private GateAdapter adapter;
    private DatabaseReference gatesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        clearActiveNav();

        RecyclerView recyclerView = findViewById(R.id.recyclerViewGates);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GateAdapter(gateList, this);
        recyclerView.setAdapter(adapter);

        FirebaseApp.initializeApp(this);
        gatesRef = FirebaseDatabase.getInstance().getReference("gates");

        setupHttpsClient();
        subscribeToFirebase();
    }

    /**
     * HTTPS client
     */
    private void setupHttpsClient() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean useSsl = prefs.getBoolean("server_ssl", false);
        if (!useSsl) {
            client = new OkHttpClient();
            return;
        }
        try {
            InputStream ksStream = getResources().openRawResource(R.raw.mykeystore);
            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(ksStream, "killnoob200".toCharArray());

            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(
                            TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            X509TrustManager trustManager =
                    (X509TrustManager) tmf.getTrustManagers()[0];

            client = new OkHttpClient.Builder()
                    .sslSocketFactory(
                            sslContext.getSocketFactory(),
                            trustManager)
                    .hostnameVerifier((h, s) -> true)
                    .build();

        } catch (Exception e) {
            client = new OkHttpClient();
        }
    }

    private String getGateControlUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String host = prefs.getString("server_host", "localhost");
        String port = prefs.getString("server_port", "8080");
        boolean ssl = prefs.getBoolean("server_ssl", false);
        String scheme = ssl ? "https" : "http";
        return scheme + "://" + host + ":" + port + "/gate/control";
    }

    /**
     * Firebase is SOURCE OF TRUTH
     */

    private void subscribeToFirebase() {
        gatesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                gateList.clear();

                for (DataSnapshot g : snapshot.getChildren()) {
                    String id = g.getKey();
                    String state = g.child("state").getValue(String.class);
                    Integer visLevel = g.child("visibilityLevel").getValue(Integer.class);
                    Integer openLevel = g.child("openableLevel").getValue(Integer.class);
                    Integer ctrlLevel = g.child("controlLevel").getValue(Integer.class);
                    Boolean disabledVal = g.child("disabled").getValue(Boolean.class);

                    Log.d(TAG, "gateID: " + id + " vislevel: " + visLevel + " openablelevel: " + openLevel + " ctrllevel: " + ctrlLevel + " disabled: " + disabledVal);
                    int vLevel = visLevel != null ? visLevel : 0;
                    int oLevel = openLevel != null ? openLevel : 0;
                    int cLevel = ctrlLevel != null ? ctrlLevel : 0;
                    boolean isDisabled = disabledVal != null && disabledVal;

                    if (isDisabled && SessionManager.getRoleLevel() < 2) continue;
                    if (!SessionManager.canViewGate(vLevel)) continue;

                    Gate gate = new Gate(
                            id,
                            state != null ? state : "closed",
                            state != null ? state : "closed",
                            0L,
                            "",
                            vLevel,
                            oLevel,
                            cLevel
                    );
                    gate.setDisabled(isDisabled);
                    gateList.add(gate);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(gatesMain.this,
                        "Firebase error",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onGateAdminSettings(Gate gate, int position) {
    }

    /**
     * UI click
     */
    @Override
    public void onGateToggle(Gate gate, int position) {
        if (!gate.isClickable()) return;

        gate.setClickable(false);

        String targetState =
                gate.isOpen() ? "closed" : "open";

        gate.setActionStatus(
                targetState.equals("open")
                        ? "Opening..."
                        : "Closing...");
        adapter.updateGate(position, gate);

        sendCommand(gate.getGateNumber(), targetState, position);
    }

    /**
     * Send command ONLY
     */
    private void sendCommand(
            String gateId,
            String targetState,
            int position) {

        try {
            JSONObject json = new JSONObject();
            json.put("gate", gateId);
            json.put("state", targetState);
            json.put("commandId", UUID.randomUUID().toString());
            json.put("userName", SessionManager.getCurrentUsername());

            Request request = new Request.Builder()
                    .url(getGateControlUrl())
                    .post(RequestBody.create(
                            json.toString(),
                            MediaType.get("application/json")))
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(
                        @NonNull Call call,
                        @NonNull IOException e) {

                    runOnUiThread(() -> {
                        Gate g = gateList.get(position);
                        g.setActionStatus(g.getState());
                        g.setClickable(true);
                        adapter.updateGate(position, g);

                        Toast.makeText(
                                gatesMain.this,
                                "Server unreachable",
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(
                        @NonNull Call call,
                        @NonNull Response response) {

                    response.close();
                    // DO NOTHING — Firebase will update UI
                }
            });

        } catch (Exception e) {
            Toast.makeText(this,
                    "Command error",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
