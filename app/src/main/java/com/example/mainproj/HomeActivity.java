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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        client = new OkHttpClient();
        btnSend = findViewById(R.id.sendToServer);
        textViewResponse = findViewById(R.id.textview);

        btnSend.setOnClickListener(v -> sendToServer("Alice"));
    }

    private void sendToServer(String name) {
        String json = "{\"name\":\"" + name + "\"}";
        Log.d("prog", "Sending JSON: " + json);

        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        // For REAL DEVICE with adb reverse: use 127.0.0.1
        // For EMULATOR: use 10.0.2.2
        Request request = new Request.Builder()
                .url("http://127.0.0.1:8080/greet")
                .post(body)
                .build();

        Log.d("prog", "Sending to: " + request.url());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("prog", "Request failed: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(HomeActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d("prog", "Response code: " + response.code());

                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(HomeActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                final String responseBody = response.body().string();
                Log.d("prog", "Success! Response: " + responseBody);

                runOnUiThread(() -> {
                    textViewResponse.setText(responseBody);
                    Toast.makeText(HomeActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}