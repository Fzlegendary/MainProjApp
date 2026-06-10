package com.example.mainproj.messaging;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.mainproj.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiManager {

    private static final String TAG = "GeminiManager";
    private static final String API_BASE =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    // How long to wait before each retry attempt (ms)
    private static final long[] RETRY_DELAYS_MS = { 10_000, 20_000, 30_000 };

    private static GeminiManager instance;
    private final OkHttpClient httpClient;
    private final String apiKey;
    private final Context context;

    private GeminiManager(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.apiKey = context.getString(R.string.gemini_api_key);
    }

    public static synchronized GeminiManager getInstance(Context context) {
        if (instance == null) {
            instance = new GeminiManager(context);
        }
        return instance;
    }

    public String buildChatId(String username) {
        return username + "_gemini_ai";
    }

    // ── Public entry point ────────────────────────────────────────────────────
    public void sendMessage(String userInput, List<MessageModel> conversationHistory,
                            String currentUsername, final GeminiCallback callback) {
        sendWithRetry(userInput, conversationHistory, 0, callback);
    }

    // ── Internal retry loop ───────────────────────────────────────────────────
    private void sendWithRetry(String userInput, List<MessageModel> conversationHistory,
                               int attempt, final GeminiCallback callback) {
        String url = API_BASE + apiKey;

        Request request;
        try {
            request = buildRequest(url, userInput, conversationHistory);
        } catch (Exception e) {
            Log.e(TAG, "Failed to build request", e);
            if (callback != null) callback.onError("Failed to build request");
            return;
        }

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed (attempt " + attempt + ")", e);
                if (callback != null) {
                    callback.onError("Network error: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // ── 429: rate limited — retry with backoff ────────────────
                if (response.code() == 429) {
                    response.close();

                    if (attempt < RETRY_DELAYS_MS.length) {
                        long delayMs = RETRY_DELAYS_MS[attempt];
                        int nextAttempt = attempt + 1;
                        int secondsLeft = (int) (delayMs / 1000);

                        Log.w(TAG, "429 rate limited. Retrying in " + secondsLeft + "s (attempt " + nextAttempt + ")");

                        // Tell the UI we're waiting so the user isn't left staring at nothing
                        if (callback != null) {
                            callback.onRateLimited(secondsLeft, nextAttempt, RETRY_DELAYS_MS.length);
                        }

                        android.os.Handler handler = new android.os.Handler(
                                android.os.Looper.getMainLooper());
                        handler.postDelayed(() ->
                                        sendWithRetry(userInput, conversationHistory, nextAttempt, callback),
                                delayMs
                        );
                    } else {
                        // All retries exhausted
                        Log.e(TAG, "429 rate limit — all retries exhausted");
                        if (callback != null) {
                            callback.onError("Too many requests. Please wait a moment and try again.");
                        }
                    }
                    return;
                }

                // ── Other HTTP error ──────────────────────────────────────
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "API error " + response.code() + ": " + errorBody);
                    if (callback != null) {
                        callback.onError("API error (" + response.code() + ")");
                    }
                    return;
                }

                // ── Success ───────────────────────────────────────────────
                try {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray candidates = jsonResponse.optJSONArray("candidates");
                    if (candidates != null && candidates.length() > 0) {
                        JSONObject candidate = candidates.getJSONObject(0);
                        JSONObject contentObj = candidate.optJSONObject("content");
                        if (contentObj != null) {
                            JSONArray partsArray = contentObj.optJSONArray("parts");
                            if (partsArray != null && partsArray.length() > 0) {
                                String text = partsArray.getJSONObject(0).optString("text", "");
                                if (callback != null) {
                                    callback.onResponse(text);
                                }
                                return;
                            }
                        }
                    }
                    if (callback != null) {
                        callback.onError("Empty response from AI");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse response", e);
                    if (callback != null) {
                        callback.onError("Failed to parse response");
                    }
                }
            }
        });
    }

    // ── Build the Gemini request ──────────────────────────────────────────────
    private Request buildRequest(String url, String userInput,
                                 List<MessageModel> conversationHistory) throws Exception {
        JSONObject json = new JSONObject();
        JSONArray contents = new JSONArray();

        for (MessageModel msg : conversationHistory) {
            String text = msg.getText();
            if (text == null || text.isEmpty()) continue;
            if ("date_separator".equals(msg.getSenderUsername())) continue;

            String role = "Gemini".equals(msg.getSenderUsername()) ? "model" : "user";
            JSONObject histContent = new JSONObject();
            JSONArray histParts = new JSONArray();
            JSONObject histPart = new JSONObject();
            histPart.put("text", text);
            histParts.put(histPart);
            histContent.put("parts", histParts);
            histContent.put("role", role);
            contents.put(histContent);
        }

        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", userInput);
        parts.put(part);
        content.put("parts", parts);
        content.put("role", "user");
        contents.put(content);
        json.put("contents", contents);

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        return new Request.Builder()
                .url(url)
                .post(body)
                .build();
    }

    // ── Firebase / cache helpers ──────────────────────────────────────────────
    public ArrayList<MessageModel> loadMessages(String chatId) {
        if (!ChatCacheManager.isCachingEnabled()) return new ArrayList<>();
        return ChatCacheManager.getInstance(context).loadMessages(chatId);
    }

    public void saveMessagesToCache(String chatId, ArrayList<MessageModel> messages) {
        ChatCacheManager.getInstance(context).saveMessages(chatId, messages);
    }

    public void saveMessageToFirebase(MessageModel msg, String messagesPath) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(messagesPath);
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", msg.getText());
        messageData.put("senderUsername", msg.getSenderUsername());
        messageData.put("timestamp", ServerValue.TIMESTAMP);
        if (msg.getImageUrl() != null) {
            messageData.put("imageUrl", msg.getImageUrl());
        }
        ref.push().setValue(messageData);
    }

    public void clearMessages(String chatId) {
        ChatCacheManager.getInstance(context).clearChat(chatId);
        FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages").removeValue();
    }

    public void archiveConversation(String username, String currentChatId,
                                    List<MessageModel> messages, ArchiveCallback callback) {
        String archiveBase = "gemini_ai/" + username;
        DatabaseReference archiveRef = FirebaseDatabase.getInstance().getReference(archiveBase);

        archiveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int nextNumber = 1;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    if (key != null && key.startsWith("chat")) {
                        try {
                            nextNumber = Math.max(nextNumber, Integer.parseInt(key.substring(4)) + 1);
                        } catch (NumberFormatException ignored) {}
                    }
                }

                String archiveChatId = "gemini_ai/" + username + "/chat" + nextNumber;
                DatabaseReference archiveMessagesRef = FirebaseDatabase.getInstance()
                        .getReference(archiveChatId).child("messages");

                int total = messages.size();
                if (total == 0) {
                    clearMessages(currentChatId);
                    if (callback != null) callback.onComplete(archiveChatId);
                    return;
                }

                final int[] savedCount = {0};
                for (MessageModel msg : messages) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("text", msg.getText() != null ? msg.getText() : "");
                    data.put("senderUsername", msg.getSenderUsername());
                    data.put("timestamp", msg.getTimestamp());
                    if (msg.getImageUrl() != null) data.put("imageUrl", msg.getImageUrl());
                    archiveMessagesRef.push().setValue(data).addOnCompleteListener(task -> {
                        savedCount[0]++;
                        if (savedCount[0] == total) {
                            clearMessages(currentChatId);
                            if (callback != null) callback.onComplete(archiveChatId);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onError(error.getMessage());
            }
        });
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────
    public interface GeminiCallback {
        void onResponse(String replyText);
        void onError(String error);

        /**
         * Called when a 429 is received and a retry is scheduled.
         * @param secondsUntilRetry how long until the next attempt
         * @param attempt           which retry this is (1-based)
         * @param maxAttempts       total retries available
         */
        default void onRateLimited(int secondsUntilRetry, int attempt, int maxAttempts) {
            // Default: no-op. Override in MessagingActivity to show UI feedback.
        }
    }

    public interface ArchiveCallback {
        void onComplete(String archiveChatId);
        void onError(String error);
    }
}