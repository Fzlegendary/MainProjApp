package com.example.mainproj;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeActivity extends AppCompatActivity {

    private OkHttpClient client;
    private Button btnSend;
    private TextView textViewResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        client = new OkHttpClient();

        btnSend = findViewById(R.id.sendToServer);
        textViewResponse = findViewById(R.id.textview);

        // Button click listener
        btnSend.setOnClickListener(v -> sendToServer("Alice"));
    }

    private void sendToServer(String name) {
        // JSON payload
        String json = "{\"name\":\"" + name + "\"}";
        Log.d("prog", "Sending JSON: " + json);

        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        // Use adb reverse to map 127.0.0.1:8080 from PC to phone if needed
        Request request = new Request.Builder()
                .url("http://192.168.21.48:8080/greet")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("prog", "Failed to receive response");
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(HomeActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(HomeActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    textViewResponse.setText(responseBody);
                    Log.d("prog", "Server response: " + responseBody);
                });
            }
        });
    }
}
