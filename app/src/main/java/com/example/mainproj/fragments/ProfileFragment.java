package com.example.mainproj.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mainproj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private boolean showingSettings = false;
    private View profileView;
    private View settingsView;
    private TextView tvUsername, tvEmail;
    private ImageView ivAvatar;

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
        ivAvatar = view.findViewById(R.id.ivAvatar);

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

    private void setupSettings() {
        View btnBack = settingsView.findViewById(R.id.btnBackToProfile);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                showingSettings = false;
                showProfile();
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