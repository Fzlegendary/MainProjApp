package com.example.mainproj;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class sign_up_activity extends AppCompatActivity {

    private TextView login;
    private TextInputEditText etUsername, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnSignUp, btnGoogleSignUp;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData()).getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (Exception e) {
                    Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnSignUp = findViewById(R.id.btnSignUp);
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp2);
        login = findViewById(R.id.tvLogin);
        login.setOnClickListener(v -> {
            Intent i = new Intent(this, Login_activity.class);
            startActivity(i);
            finish();
        });

        // Google Sign-In initialization
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // must match Firebase console
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnSignUp.setOnClickListener(v -> registerUser());
        btnGoogleSignUp.setOnClickListener(v -> googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent()));
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create FirebaseAuth account first
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = mAuth.getCurrentUser().getUid();
                    String safeEmail = email.replace(".", "_");

                    String passwordHash = hashPassword(password);
                    String registrationTime = timestampNow();

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("username", username);
                    userData.put("email", email);
                    userData.put("uid", uid);
                    userData.put("role", "user");
                    userData.put("password_hash", passwordHash);
                    userData.put("registration_time", registrationTime);

                    FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(safeEmail)
                            .setValue(userData)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(sign_up_activity.this, HomeActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to save user", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
                    if (account == null) return;

                    String safeEmail = account.getEmail().replace(".", "_");
                    String uid = mAuth.getCurrentUser().getUid();
                    String registrationTime = timestampNow();

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("username", account.getDisplayName());
                    userData.put("email", account.getEmail());
                    userData.put("registration_time", registrationTime);
                    userData.put("role", "user");
                    userData.put("uid", uid);

                    FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(safeEmail)
                            .setValue(userData)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Google account created", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(sign_up_activity.this, HomeActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed saving user", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Google authentication failed", Toast.LENGTH_SHORT).show());
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String timestampNow() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
