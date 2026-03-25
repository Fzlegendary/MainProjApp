package com.example.mainproj.userSystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mainproj.MainActivity;
import com.example.mainproj.R;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.*;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login_activity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogleLogin;
    private TextView tvSignUp;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                            Toast.makeText(this, "Google sign-in cancelled", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        try {
                            GoogleSignInAccount account =
                                    GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                                            .getResult(ApiException.class);

                            firebaseAuthWithGoogle(account.getIdToken());

                        } catch (ApiException e) {
                            Toast.makeText(this,
                                    "Google login failed: " + e.getStatusCode(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.mainproj.R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(com.example.mainproj.R.id.etEmail);
        etPassword = findViewById(com.example.mainproj.R.id.etPassword);
        btnLogin = findViewById(com.example.mainproj.R.id.btnLogin);
        btnGoogleLogin = findViewById(com.example.mainproj.R.id.btnGoogleLogin);
        tvSignUp = findViewById(com.example.mainproj.R.id.tvSignUp);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogleLogin.setOnClickListener(v -> {
            mGoogleSignInClient.signOut();
            googleLauncher.launch(mGoogleSignInClient.getSignInIntent());
        });

        tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, sign_up_activity.class))
        );
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
