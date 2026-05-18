package com.example.mainproj.userSystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mainproj.MainActivity;
import com.example.mainproj.R;
import com.example.mainproj.auth.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class sign_up_activity extends AppCompatActivity implements
        FragmentSignUpEntry.OnEntryCompleteListener,
        FragmentStep1Phone.OnPhoneCompleteListener,
        FragmentStep2Username.OnUsernameCompleteListener,
        FragmentStep3Age.OnAgeCompleteListener,
        FragmentStep4Verify.OnVerificationCompleteListener {

    private ProgressBar progressBar;
    private TextView tvStepIndicator;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private int currentStep = 0;
    private String email, password;
    private String phone = "";
    private boolean phoneSkipped = false;
    private String username = "";
    private String nickname = "";
    private int age = 0;
    private boolean phoneVerified = false;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    Toast.makeText(this, "Google sign-in cancelled", Toast.LENGTH_SHORT).show();
                    return;
                }
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

        progressBar = findViewById(R.id.progressBar);
        tvStepIndicator = findViewById(R.id.tvStepIndicator);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        showEntryScreen();
    }

    private void updateProgress(int step) {
        currentStep = step;
        progressBar.setProgress(step);
        tvStepIndicator.setText("Step " + step + " of 4");
    }

    private void showEntryScreen() {
        updateProgress(1);
        FragmentSignUpEntry fragment = new FragmentSignUpEntry();
        fragment.setOnEntryCompleteListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    public void onEmailPasswordContinue(String email, String password) {
        this.email = email;
        this.password = password;

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    mAuth.getCurrentUser().sendEmailVerification();
                    startStep1Phone();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Sign up failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    public void onGoogleSignUp() {
        googleLauncher.launch(mGoogleSignInClient.getSignInIntent());
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        email = user.getEmail();
                        username = user.getDisplayName();
                    }
                    checkIfUserExistsAndProceed();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Google authentication failed", Toast.LENGTH_SHORT).show());
    }

    private void checkIfUserExistsAndProceed() {
        if (email == null) return;
        String safeEmail = email.replace(".", "_");

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.child(safeEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(sign_up_activity.this, "Account already exists", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(sign_up_activity.this, MainActivity.class));
                    finish();
                } else {
                    startStep1Phone();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                startStep1Phone();
            }
        });
    }

    @Override
    public void onLoginClick() {
        startActivity(new Intent(this, Login_activity.class));
        finish();
    }

    private void startStep1Phone() {
        updateProgress(2);
        FragmentStep1Phone fragment = new FragmentStep1Phone();
        fragment.setOnPhoneCompleteListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    public void onPhoneNext(String phone) {
        this.phone = phone;
        this.phoneSkipped = false;
        startStep2Username();
    }

    @Override
    public void onPhoneSkip() {
        this.phone = "";
        this.phoneSkipped = true;
        startStep2Username();
    }

    private void startStep2Username() {
        updateProgress(3);
        FragmentStep2Username fragment = new FragmentStep2Username();
        fragment.setOnUsernameCompleteListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    public void onUsernameNext(String username, String nickname) {
        this.username = username;
        this.nickname = nickname;
        startStep3Age();
    }

    private void startStep3Age() {
        updateProgress(4);
        FragmentStep3Age fragment = new FragmentStep3Age();
        fragment.setOnAgeCompleteListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    public void onAgeNext(int age) {
        this.age = age;

        if (phone.isEmpty() || phoneSkipped) {
            saveUserData();
        } else {
            startStep4Verify();
        }
    }

    private void startStep4Verify() {
        FragmentStep4Verify fragment = new FragmentStep4Verify();
        fragment.setPhoneNumber(phone);
        fragment.setOnVerificationCompleteListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    public void onVerificationComplete(boolean verified) {
        this.phoneVerified = verified;
        saveUserData();
    }

    private void saveUserData() {
        if (mAuth.getCurrentUser() == null || email == null) {
            Toast.makeText(this, "Error: Not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String safeEmail = email.replace(".", "_");
        String uid = mAuth.getCurrentUser().getUid();

        if (!username.isEmpty()) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build();
            mAuth.getCurrentUser().updateProfile(profileUpdates);
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("username", username);
        if (!nickname.isEmpty()) {
            userData.put("nickname", nickname);
        }
        if (!phone.isEmpty()) {
            userData.put("phone", phone);
        }
        userData.put("phoneVerified", phoneVerified);
        userData.put("age", age);
        userData.put("uid", uid);
        userData.put("role", "user");

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.child(safeEmail).setValue(userData)
                .addOnSuccessListener(unused -> {
                    DatabaseReference usernamesRef = FirebaseDatabase.getInstance().getReference("usernames");
                    usernamesRef.child(username.toLowerCase()).setValue(true);

                    SessionManager.refresh();
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(sign_up_activity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
