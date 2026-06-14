package com.example.mainproj.userSystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.mainproj.R;
import com.example.mainproj.auth.SessionManager;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.Executors;

public class Login_activity extends AppCompatActivity {

    private static final String PREFS_NAME = "MainProjPrefs";
    private static final String KEY_LOGGED_IN_EMAIL = "logged_in_email";

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogleLogin;
    private TextView tvSignUp;

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.mainproj.R.layout.activity_login);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        etEmail = findViewById(com.example.mainproj.R.id.etEmail);
        etPassword = findViewById(com.example.mainproj.R.id.etPassword);
        btnLogin = findViewById(com.example.mainproj.R.id.btnLogin);
        btnGoogleLogin = findViewById(com.example.mainproj.R.id.btnGoogleLogin);
        tvSignUp = findViewById(com.example.mainproj.R.id.tvSignUp);

        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());

        tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, sign_up_activity.class))
        );
    }

    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        runOnUiThread(() -> handleGoogleCredential(result));
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        runOnUiThread(() ->
                                Toast.makeText(Login_activity.this,
                                        "Google sign-in failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }

    private void handleGoogleCredential(GetCredentialResponse response) {
        Credential credential = response.getCredential();

        if (credential instanceof CustomCredential) {
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(credential.getType())) {

                GoogleIdTokenCredential googleCredential =
                        GoogleIdTokenCredential.createFrom(credential.getData());
                firebaseAuthWithGoogle(googleCredential.getIdToken());

            } else {
                Toast.makeText(this, "Unsupported credential type", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Unrecognized credential", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null && !user.isEmailVerified()) {
                        showVerificationToast();
                    }
                    ensureUserNodeAndProceed(email, true);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Login failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    String email = result.getUser().getEmail();
                    if (email != null) {
                        checkGoogleUserAndProceed(email);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Google auth failed",
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void checkGoogleUserAndProceed(String email) {
        String safeEmail = email.replace(".", "_");

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(safeEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            ensureUserNodeAndProceed(email, false);
                        } else {
                            Toast.makeText(Login_activity.this, "Account not found, please sign up", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(Login_activity.this, sign_up_activity.class);
                            intent.putExtra("google_email", email);
                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        ensureUserNodeAndProceed(email, false);
                    }
                });
    }

    private void ensureUserNodeAndProceed(String email, boolean checkVerification) {
        String safeEmail = email.replace(".", "_");

        prefs.edit().putString(KEY_LOGGED_IN_EMAIL, email).apply();
        SessionManager.refresh();

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(safeEmail)
                .child("email")
                .setValue(email)
                .addOnCompleteListener(task -> {
                    if (checkVerification) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && !user.isEmailVerified()) {
                            showVerificationToast();
                        }
                    }
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
    }

    private void showVerificationToast() {
        View toastView = LayoutInflater.from(this).inflate(R.layout.toast_verify_email, null);
        Toast toast = new Toast(this);
        toast.setView(toastView);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }
}
