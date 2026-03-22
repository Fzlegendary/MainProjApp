package com.example.mainproj.lists;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mainproj.shared_lists_adapter_placeholder.PlaceholderContent;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class ListRequestManager {
    private static final String TAG = "ListRequestManager";
    private final String baseUrl;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final boolean trustAllCertificates;

    public ListRequestManager(String baseUrl, boolean trustAllCertificates) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.trustAllCertificates = trustAllCertificates;
        if (trustAllCertificates) disableSSLCertificateChecking();
    }

    public interface ListCallback {
        void onSuccess(String commandId, String itemId);
        void onRejected(String reason, String commandId);
        void onError(String errorMessage);
    }

    public void addItem(String listType, PlaceholderContent.PlaceholderItem item, String userName, ListCallback callback) {
        String commandId = UUID.randomUUID().toString();
        try {
            JSONObject itemJson = new JSONObject();
            itemJson.put("name", item.name);
            itemJson.put("reason", item.reason);
            itemJson.put("expiresAt", item.timestampExpiresAt);

            JSONObject payload = new JSONObject();
            payload.put("listType", listType);
            payload.put("userName", userName);
            payload.put("commandId", commandId);
            payload.put("item", itemJson);

            executeRequest("list/add", payload, callback, commandId);
        } catch (Exception e) {
            postError(callback, "JSON build error: " + e.getMessage());
        }
    }

    public void removeItem(String listType, String itemId, String userName, ListCallback callback) {
        String commandId = UUID.randomUUID().toString();
        try {
            JSONObject payload = new JSONObject();
            payload.put("listType", listType);
            payload.put("itemId", itemId);
            payload.put("userName", userName);
            payload.put("commandId", commandId);

            executeRequest("list/remove", payload, callback, commandId);
        } catch (Exception e) {
            postError(callback, "JSON build error: " + e.getMessage());
        }
    }

    public void updateItem(String listType, String itemId, Map<String, Object> updates, String userName, ListCallback callback) {
        String commandId = UUID.randomUUID().toString();
        try {
            JSONObject updatesJson = new JSONObject(updates);

            JSONObject payload = new JSONObject();
            payload.put("listType", listType);
            payload.put("itemId", itemId);
            payload.put("userName", userName);
            payload.put("commandId", commandId);
            payload.put("updates", updatesJson);

            executeRequest("list/update", payload, callback, commandId);
        } catch (Exception e) {
            postError(callback, "JSON build error: " + e.getMessage());
        }
    }

    private void executeRequest(String endpoint, JSONObject jsonPayload, ListCallback callback, String commandId) {
        executor.execute(() -> {
            try {
                URL url = new URL(baseUrl + endpoint);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(jsonPayload.toString());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(responseCode == 200 ? conn.getInputStream() : conn.getErrorStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                if (responseCode == 200) {
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String status = jsonResponse.optString("status");
                    String recvCmd = jsonResponse.optString("commandId");
                    String reason = jsonResponse.optString("reason", null);
                    String itemId = jsonResponse.optString("itemId", null);
                    if ("acknowledged".equals(status)) {
                        postSuccess(callback, recvCmd, itemId);
                    } else {
                        postRejected(callback, reason, recvCmd);
                    }
                } else {
                    postError(callback, "HTTP error " + responseCode + ": " + response);
                }
            } catch (Exception e) {
                Log.e(TAG, "Network error", e);
                postError(callback, "Network error: " + e.getMessage());
            }
        });
    }

    private void postSuccess(ListCallback cb, String cmd, String itemId) {
        mainHandler.post(() -> cb.onSuccess(cmd, itemId));
    }

    private void postRejected(ListCallback cb, String reason, String cmd) {
        mainHandler.post(() -> cb.onRejected(reason, cmd));
    }

    private void postError(ListCallback cb, String error) {
        mainHandler.post(() -> cb.onError(error));
    }

    private void disableSSLCertificateChecking() {
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                    }
            };
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            Log.e(TAG, "SSL bypass error", e);
        }
    }
}