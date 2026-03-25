package com.example.mainproj.userSystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mainproj.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class FragmentSignUpEntry extends Fragment {

    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnContinue, btnGoogleSignUp;
    private TextView tvLogin;

    private OnEntryCompleteListener listener;

    public interface OnEntryCompleteListener {
        void onEmailPasswordContinue(String email, String password);
        void onGoogleSignUp();
        void onLoginClick();
    }

    public void setOnEntryCompleteListener(OnEntryCompleteListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_entry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        btnContinue = view.findViewById(R.id.btnContinue);
        btnGoogleSignUp = view.findViewById(R.id.btnGoogleSignUp);
        tvLogin = view.findViewById(R.id.tvLogin);

        btnContinue.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (listener != null) {
                listener.onEmailPasswordContinue(email, password);
            }
        });

        btnGoogleSignUp.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGoogleSignUp();
            }
        });

        tvLogin.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLoginClick();
            }
        });
    }
}
