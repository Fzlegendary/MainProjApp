package com.example.mainproj.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mainproj.R;
import com.example.mainproj.SharedPrefDisplayerActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private boolean showingSettings = false;
    private View profileView;
    private View settingsView;
    private TextView tvUsername, tvEmail, tvBio;
    private EditText etBioInput;
    private MaterialButton btnSaveBio;
    private ImageView ivAvatar;
    private View loadingState;

    private String safeEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileView = view.findViewById(R.id.profileContent);
        settingsView = view.findViewById(R.id.settingsContent);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvBio = view.findViewById(R.id.tvBio);
        loadingState = view.findViewById(R.id.loadingState);
        ivAvatar = view.findViewById(R.id.ivAvatar);
        etBioInput = view.findViewById(R.id.etBioInput);
        btnSaveBio = view.findViewById(R.id.btnSaveBio);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            safeEmail = user.getEmail().replace(".", "_");
            loadBio();
        }

        btnSaveBio.setOnClickListener(v -> saveBio());

        setupProfile();
        setupSettings();
        showProfile();
    }

    private void setupProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (user.getDisplayName() != null) {
                tvUsername.setText(user.getDisplayName());
            } else {
                tvUsername.setText("User");
            }
            if (user.getEmail() != null) {
                tvEmail.setText(user.getEmail());
            }
            if (user.getPhotoUrl() != null) {
                ivAvatar.setImageURI(user.getPhotoUrl());
            }
        }
    }

    private void loadBio() {
        if (safeEmail == null) return;
        DatabaseReference bioRef = FirebaseDatabase.getInstance()
                .getReference("users").child(safeEmail).child("bio");
        bioRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (loadingState != null) loadingState.setVisibility(View.GONE);
                String bio = snapshot.getValue(String.class);
                if (bio != null && !bio.isEmpty()) {
                    tvBio.setText(bio);
                    etBioInput.setText(bio);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (loadingState != null) loadingState.setVisibility(View.GONE);
            }
        });
    }

    private void saveBio() {
        if (safeEmail == null) return;
        String bio = etBioInput.getText().toString().trim();
        DatabaseReference bioRef = FirebaseDatabase.getInstance()
                .getReference("users").child(safeEmail).child("bio");
        bioRef.setValue(bio).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                tvBio.setText(bio.isEmpty() ? "No bio yet." : bio);
                Toast.makeText(requireContext(), "Bio saved", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to save bio", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSettings() {
        View btnBack = settingsView.findViewById(R.id.btnBackToProfile);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                showingSettings = false;
                showProfile();
            });
        }

        Button btnPrefViewer = settingsView.findViewById(R.id.btnSharedPrefViewer);
        if (btnPrefViewer != null) {
            btnPrefViewer.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), SharedPrefDisplayerActivity.class);
                startActivity(intent);
            });
        }
    }

    public void showProfile() {
        profileView.setVisibility(View.VISIBLE);
        settingsView.setVisibility(View.GONE);
    }

    public void showSettingsInstead() {
        showingSettings = true;
        profileView.setVisibility(View.GONE);
        settingsView.setVisibility(View.VISIBLE);
        String currentBio = tvBio.getText().toString();
        if (currentBio.equals("No bio yet.")) currentBio = "";
        if (etBioInput.getText().toString().trim().isEmpty()) {
            etBioInput.setText(currentBio);
        }
    }

    public boolean isShowingSettings() {
        return showingSettings;
    }

    public void onTabReselected() {
        if (!showingSettings) {
            showSettingsInstead();
        } else {
            showProfile();
        }
    }
}