package com.example.mainproj;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom);
                    return insets;
                });

        MaterialButton btnMessaging = findViewById(R.id.btnMessaging);
        MaterialButton btnGateControl = findViewById(R.id.btnGateControl);
        MaterialButton btnServerSettings = findViewById(R.id.btnServerSettings);
        MaterialButton btnAccountManagement = findViewById(R.id.btnAccountManagement);
        MaterialButton btnBlacklist = findViewById(R.id.btnBlacklist);
        MaterialButton btnWhitelist = findViewById(R.id.btnWhitelist);

        btnMessaging.setOnClickListener(v ->
                startActivity(new Intent(this, MessagingActivity.class)));

        btnGateControl.setOnClickListener(v ->
                startActivity(new Intent(this, HomeActivity.class)));

        btnServerSettings.setOnClickListener(v ->
                startActivity(new Intent(this, ServerSettingsActivity.class)));

        btnAccountManagement.setOnClickListener(v ->
                startActivity(new Intent(this, AccountManagementActivity.class)));

        btnBlacklist.setOnClickListener(v ->
                startActivity(new Intent(this, listsActivity.class)));
    }
}
