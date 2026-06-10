package com.example.mainproj.fragments.subnav;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mainproj.R;
import com.example.mainproj.auth.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ServerSettingsFragment extends Fragment {

    private static final String PREFS_NAME       = "MainProjPrefs";
    private static final String KEY_HOST         = "server_host";
    private static final String KEY_PORT         = "server_port";
    private static final String KEY_SHUTDOWN_KEY = "server_shutdown_key";
    // Flag: user has set a local override (takes priority over Firebase)
    private static final String KEY_LOCAL_OVERRIDE = "server_local_override";

    // Firebase path where the global server config lives
    private static final String FB_SERVER_CONFIG = "server_config";

    private SharedPreferences prefs;
    private OkHttpClient client;

    private TextInputEditText etHost;
    private TextInputEditText etPort;
    private TextInputEditText etShutdownKey;
    private MaterialButton btnStatus;
    private MaterialButton btnToggle;
    private MaterialButton btnSaveLocal;
    private MaterialButton btnPushToFirebase;
    private MaterialButton btnClearLocalOverride;
    private TextView tvStatus;
    private TextView tvConfigSource;   // shows "Using: Firebase config" or "Using: Local override"

    private boolean serverEnabled = true;
    private int currentUserRoleLevel = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sub_server_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs  = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        client = new OkHttpClient();

        etHost             = view.findViewById(R.id.etServerHost);
        etPort             = view.findViewById(R.id.etServerPort);
        etShutdownKey      = view.findViewById(R.id.etShutdownKey);
        btnStatus          = view.findViewById(R.id.btnCheckStatus);
        btnToggle          = view.findViewById(R.id.btnToggleServer);
        btnSaveLocal       = view.findViewById(R.id.btnSaveLocalOverride);
        btnPushToFirebase  = view.findViewById(R.id.btnPushToFirebase);
        btnClearLocalOverride = view.findViewById(R.id.btnClearLocalOverride);
        tvStatus           = view.findViewById(R.id.tvServerStatus);
        tvConfigSource     = view.findViewById(R.id.tvConfigSource);

        // Load role level, then load config
        loadRoleLevelThenConfig();

        btnStatus.setOnClickListener(v -> checkStatus());
        btnToggle.setOnClickListener(v -> onToggleClicked());

        // Save host/port as a local override (anyone can do this)
        btnSaveLocal.setOnClickListener(v -> saveLocalOverride());

        // Clear local override — revert to Firebase values
        btnClearLocalOverride.setOnClickListener(v -> clearLocalOverride());

        // Push local values up to Firebase — superadmin only
        btnPushToFirebase.setOnClickListener(v -> pushToFirebase());
    }

    // ── Role + config loading ─────────────────────────────────────────────────

    private void loadRoleLevelThenConfig() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            loadConfig();
            return;
        }
        String safeEmail = user.getEmail().replace(".", "_");
        FirebaseDatabase.getInstance()
                .getReference("users").child(safeEmail).child("roleLevel")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long level = snapshot.getValue(Long.class);
                        currentUserRoleLevel = level != null ? level.intValue() : 0;
                        loadConfig();
                        updateRoleBasedUI();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        currentUserRoleLevel = 0;
                        loadConfig();
                        updateRoleBasedUI();
                    }
                });
    }

    /**
     * Priority: local override > Firebase config > hardcoded defaults.
     */
    private void loadConfig() {
        if (prefs.getBoolean(KEY_LOCAL_OVERRIDE, false)) {
            // Local override is active — just show what's stored
            etHost.setText(prefs.getString(KEY_HOST, "localhost"));
            etPort.setText(prefs.getString(KEY_PORT, "8080"));
            etShutdownKey.setText(prefs.getString(KEY_SHUTDOWN_KEY, ""));
            setConfigSource("Local override");
            return;
        }

        // Pull from Firebase and store locally (no override flag set)
        setConfigSource("Loading from Firebase...");
        FirebaseDatabase.getInstance()
                .getReference(FB_SERVER_CONFIG)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String host = snapshot.child("host").getValue(String.class);
                        String port = snapshot.child("port").getValue(String.class);
                        String key  = snapshot.child("shutdownKey").getValue(String.class);

                        if (host != null) prefs.edit().putString(KEY_HOST, host).apply();
                        if (port != null) prefs.edit().putString(KEY_PORT, port).apply();
                        if (key  != null) prefs.edit().putString(KEY_SHUTDOWN_KEY, key).apply();

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                etHost.setText(prefs.getString(KEY_HOST, "localhost"));
                                etPort.setText(prefs.getString(KEY_PORT, "8080"));
                                etShutdownKey.setText(prefs.getString(KEY_SHUTDOWN_KEY, ""));
                                setConfigSource("Firebase config");
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Fall back to whatever is stored locally
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                etHost.setText(prefs.getString(KEY_HOST, "localhost"));
                                etPort.setText(prefs.getString(KEY_PORT, "8080"));
                                etShutdownKey.setText(prefs.getString(KEY_SHUTDOWN_KEY, ""));
                                setConfigSource("Firebase unavailable — using cached");
                            });
                        }
                    }
                });
    }

    // ── Local override ────────────────────────────────────────────────────────

    private void saveLocalOverride() {
        String host = etHost.getText() != null ? etHost.getText().toString().trim() : "";
        String port = etPort.getText() != null ? etPort.getText().toString().trim() : "";
        String key  = etShutdownKey.getText() != null ? etShutdownKey.getText().toString() : "";

        if (host.isEmpty() || port.isEmpty()) {
            Toast.makeText(requireContext(), "Host and port cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        prefs.edit()
                .putString(KEY_HOST, host)
                .putString(KEY_PORT, port)
                .putString(KEY_SHUTDOWN_KEY, key)
                .putBoolean(KEY_LOCAL_OVERRIDE, true)
                .apply();

        setConfigSource("Local override");
        Toast.makeText(requireContext(), "Local override saved", Toast.LENGTH_SHORT).show();
    }

    private void clearLocalOverride() {
        prefs.edit().putBoolean(KEY_LOCAL_OVERRIDE, false).apply();
        Toast.makeText(requireContext(), "Local override cleared — reloading from Firebase", Toast.LENGTH_SHORT).show();
        loadConfig();
    }

    // ── Push to Firebase (roleLevel = 2 only) ─────────────────────────────────

    private void pushToFirebase() {
        if (currentUserRoleLevel < 2) {
            Toast.makeText(requireContext(),
                    "Only superadmins can update the global server config", Toast.LENGTH_SHORT).show();
            return;
        }

        String host = etHost.getText() != null ? etHost.getText().toString().trim() : "";
        String port = etPort.getText() != null ? etPort.getText().toString().trim() : "";
        String key  = etShutdownKey.getText() != null ? etShutdownKey.getText().toString() : "";

        if (host.isEmpty() || port.isEmpty()) {
            Toast.makeText(requireContext(), "Host and port cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Push to Firebase")
                .setMessage("Update the global server config to:\n\nHost: " + host + "\nPort: " + port
                        + "\n\nAll users without a local override will use these values.")
                .setPositiveButton("Push", (dialog, which) -> {
                    DatabaseReference ref = FirebaseDatabase.getInstance()
                            .getReference(FB_SERVER_CONFIG);
                    ref.child("host").setValue(host);
                    ref.child("port").setValue(port);
                    if (!key.isEmpty()) ref.child("shutdownKey").setValue(key);

                    Toast.makeText(requireContext(),
                            "Global config updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void updateRoleBasedUI() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            // Only superadmins see the "Push to Firebase" button
            btnPushToFirebase.setVisibility(
                    currentUserRoleLevel >= 2 ? View.VISIBLE : View.GONE);
        });
    }

    private void setConfigSource(String source) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> tvConfigSource.setText("Using: " + source));
        }
    }

    private String getBaseUrl() {
        String host = prefs.getString(KEY_HOST, "localhost");
        String port = prefs.getString(KEY_PORT, "8080");
        return "http://" + host + ":" + port;
    }

    private String getShutdownKey() {
        return prefs.getString(KEY_SHUTDOWN_KEY, "");
    }

    private void addUserIdentity(JSONObject json) throws org.json.JSONException {
        json.put("userName", SessionManager.getCurrentUsername());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            json.put("email",   user.getEmail()  != null ? user.getEmail()  : "");
            json.put("userUID", user.getUid());
        } else {
            json.put("email",   "");
            json.put("userUID", "");
        }
    }

    private void setStatus(String text) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> tvStatus.setText(text));
        }
    }

    private void flushFields() {
        // Only persist to prefs — don't touch the override flag here
        String host = etHost.getText() != null ? etHost.getText().toString().trim() : "localhost";
        String port = etPort.getText() != null ? etPort.getText().toString().trim() : "8080";
        String key  = etShutdownKey.getText() != null ? etShutdownKey.getText().toString() : "";
        if (host.isEmpty()) host = "localhost";
        if (port.isEmpty()) port = "8080";
        prefs.edit()
                .putString(KEY_HOST, host)
                .putString(KEY_PORT, port)
                .putString(KEY_SHUTDOWN_KEY, key)
                .apply();
    }

    // ── Server control ────────────────────────────────────────────────────────

    private void checkStatus() {
        flushFields();
        String url = getBaseUrl() + "/server/control";
        setStatus("Checking status...");

        try {
            JSONObject json = new JSONObject();
            json.put("action", "status");
            json.put("shutdownKey", getShutdownKey());
            addUserIdentity(json);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(json.toString(), MediaType.get("application/json")))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    setStatus("Connection failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try {
                        String body = response.body() != null ? response.body().string() : "";
                        if (response.isSuccessful()) {
                            JSONObject resp = new JSONObject(body);
                            String enabled = resp.optString("enabled", "unknown");
                            String host    = resp.optString("host",    "unknown");
                            String port    = resp.optString("port",    "unknown");
                            String uptime  = resp.optString("uptime",  "unknown");
                            setStatus("Enabled: " + enabled
                                    + "\nHost: " + host
                                    + "\nPort: " + port
                                    + "\nUptime: " + uptime);
                            serverEnabled = "true".equalsIgnoreCase(enabled);
                            if (getActivity() != null) getActivity().runOnUiThread(() -> updateToggleButton());
                        } else {
                            JSONObject err = new JSONObject(body);
                            setStatus("Error " + response.code() + ": " + err.optString("reason", response.message()));
                        }
                    } catch (Exception e) {
                        setStatus("Parse error: " + e.getMessage());
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            setStatus("Request error: " + e.getMessage());
        }
    }

    private void updateToggleButton() {
        if (serverEnabled) {
            btnToggle.setText("Disable Server");
            btnToggle.setTextColor(getResources().getColor(R.color.colorToggleDisabled, requireContext().getTheme()));
            btnToggle.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.colorToggleDisabled, requireContext().getTheme())));
        } else {
            btnToggle.setText("Enable Server");
            btnToggle.setTextColor(getResources().getColor(R.color.colorToggleEnabled, requireContext().getTheme()));
            btnToggle.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.colorToggleEnabled, requireContext().getTheme())));
        }
    }

    private void onToggleClicked() {
        if (serverEnabled) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Disable Server")
                    .setMessage("Are you sure you want to disable the server?")
                    .setPositiveButton("Disable", (dialog, which) -> sendControlAction("disable"))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            sendControlAction("enable");
        }
    }

    private void sendControlAction(String action) {
        flushFields();
        String url = getBaseUrl() + "/server/control";
        String key = getShutdownKey();

        if (TextUtils.isEmpty(key)) {
            setStatus("Shutdown key is required");
            return;
        }

        setStatus("Sending " + action + "...");

        try {
            JSONObject json = new JSONObject();
            json.put("action", action);
            json.put("shutdownKey", key);
            addUserIdentity(json);
            json.put("commandId", UUID.randomUUID().toString());

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(json.toString(), MediaType.get("application/json")))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    setStatus("Failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try {
                        String body = response.body() != null ? response.body().string() : "";
                        if (response.isSuccessful()) {
                            JSONObject resp = new JSONObject(body);
                            setStatus(resp.optString("message", action + " successful"));
                            serverEnabled = "enable".equals(action);
                            if (getActivity() != null) getActivity().runOnUiThread(() -> updateToggleButton());
                        } else {
                            JSONObject err = new JSONObject(body);
                            setStatus("Error " + response.code() + ": " + err.optString("reason", response.message()));
                        }
                    } catch (Exception e) {
                        setStatus("Parse error: " + e.getMessage());
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            setStatus("Request error: " + e.getMessage());
        }
    }
}