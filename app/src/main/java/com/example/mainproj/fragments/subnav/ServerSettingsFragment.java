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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.mainproj.R;
import com.example.mainproj.auth.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

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

    private static final String PREFS_NAME = "MainProjPrefs";
    private static final String KEY_HOST = "server_host";
    private static final String KEY_PORT = "server_port";
    private static final String KEY_SSL = "server_ssl";
    private static final String KEY_SHUTDOWN_KEY = "server_shutdown_key";

    private SharedPreferences prefs;
    private OkHttpClient client;

    private TextInputEditText etHost;
    private TextInputEditText etPort;
    private SwitchCompat switchSsl;
    private TextInputEditText etShutdownKey;
    private MaterialButton btnStatus;
    private MaterialButton btnToggle;
    private TextView tvStatus;

    private boolean serverEnabled = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sub_server_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        client = new OkHttpClient();

        etHost = view.findViewById(R.id.etServerHost);
        etPort = view.findViewById(R.id.etServerPort);
        switchSsl = view.findViewById(R.id.switchUseSsl);
        etShutdownKey = view.findViewById(R.id.etShutdownKey);
        btnStatus = view.findViewById(R.id.btnCheckStatus);
        btnToggle = view.findViewById(R.id.btnToggleServer);
        tvStatus = view.findViewById(R.id.tvServerStatus);

        loadSettings();

        etHost.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) saveHost(); });
        etPort.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) savePort(); });
        switchSsl.setOnCheckedChangeListener((buttonView, isChecked) -> saveSsl(isChecked));
        etShutdownKey.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) saveShutdownKey(); });

        btnStatus.setOnClickListener(v -> checkStatus());
        btnToggle.setOnClickListener(v -> onToggleClicked());
    }

    private String getBaseUrl() {
        String host = prefs.getString(KEY_HOST, "localhost");
        String port = prefs.getString(KEY_PORT, "8080");
        boolean ssl = prefs.getBoolean(KEY_SSL, false);
        String scheme = ssl ? "https" : "http";
        return scheme + "://" + host + ":" + port;
    }

    private void loadSettings() {
        etHost.setText(prefs.getString(KEY_HOST, "localhost"));
        etPort.setText(prefs.getString(KEY_PORT, "8080"));
        switchSsl.setChecked(prefs.getBoolean(KEY_SSL, false));
        etShutdownKey.setText(prefs.getString(KEY_SHUTDOWN_KEY, "123"));
    }

    private void saveHost() {
        String val = etHost.getText() != null ? etHost.getText().toString().trim() : "localhost";
        if (val.isEmpty()) val = "localhost";
        prefs.edit().putString(KEY_HOST, val).apply();
    }

    private void savePort() {
        String val = etPort.getText() != null ? etPort.getText().toString().trim() : "8080";
        if (val.isEmpty()) val = "8080";
        prefs.edit().putString(KEY_PORT, val).apply();
    }

    private void saveSsl(boolean enabled) {
        prefs.edit().putBoolean(KEY_SSL, enabled).apply();
    }

    private void saveShutdownKey() {
        String val = etShutdownKey.getText() != null ? etShutdownKey.getText().toString() : "";
        prefs.edit().putString(KEY_SHUTDOWN_KEY, val).apply();
    }

    private String getShutdownKey() {
        return prefs.getString(KEY_SHUTDOWN_KEY, "123");
    }

    private void addUserIdentity(JSONObject json) throws org.json.JSONException {
        json.put("userName", SessionManager.getCurrentUsername());
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            json.put("email", FirebaseAuth.getInstance().getCurrentUser().getEmail() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "");
            json.put("userUID", FirebaseAuth.getInstance().getCurrentUser().getUid());
        } else {
            json.put("email", "");
            json.put("userUID", "");
        }
    }

    private void setStatus(String text) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> tvStatus.setText(text));
        }
    }

    private void flushFields() {
        saveHost();
        savePort();
        saveShutdownKey();
    }

    private void checkStatus() {
        flushFields();
        String url = getBaseUrl() + "/server/control";
        String key = getShutdownKey();
        setStatus("Checking status...");

        try {
            JSONObject json = new JSONObject();
            json.put("action", "status");
            json.put("shutdownKey", key);
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
                            String host = resp.optString("host", "unknown");
                            String port = resp.optString("port", "unknown");
                            String uptime = resp.optString("uptime", "unknown");
                            setStatus("Enabled: " + enabled + "\nHost: " + host + "\nPort: " + port + "\nUptime: " + uptime);
                            serverEnabled = "true".equalsIgnoreCase(enabled);
                            updateToggleButton();
                        } else {
                            JSONObject err = new JSONObject(body);
                            String detail = err.optString("error", response.message());
                            setStatus("Error " + response.code() + ": " + detail);
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
            btnToggle.setTextColor(0xFFD32F2F);
            btnToggle.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFD32F2F));
        } else {
            btnToggle.setText("Enable Server");
            btnToggle.setTextColor(0xFF388E3C);
            btnToggle.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFF388E3C));
        }
    }

    private void onToggleClicked() {
        if (serverEnabled) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Disable Server")
                    .setMessage("Are you sure you want to disable the server? This will stop it.")
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
                            String msg = resp.optString("message", action + " successful");
                            setStatus(msg);
                            serverEnabled = "enable".equals(action);
                            updateToggleButton();
                        } else {
                            JSONObject err = new JSONObject(body);
                            String detail = err.optString("error", response.message());
                            setStatus("Error " + response.code() + ": " + detail);
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
