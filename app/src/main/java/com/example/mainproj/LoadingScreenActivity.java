package com.example.mainproj;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mainproj.userSystem.Login_activity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoadingScreenActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_screen);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Intent intent;
            if (currentUser != null && currentUser.getEmail() != null) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, Login_activity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }
}
