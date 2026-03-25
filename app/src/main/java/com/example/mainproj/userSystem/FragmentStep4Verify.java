package com.example.mainproj.userSystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mainproj.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class FragmentStep4Verify extends Fragment {

    private TextView tvPhoneNumber, tvVerified;
    private MaterialButton btnSendSms, btnVerify, btnSkip;
    private TextInputEditText etVerificationCode;
    private ProgressBar progressBar;

    private String phoneNumber;
    private String verificationId;
    private boolean isVerified = false;

    private OnVerificationCompleteListener listener;

    public interface OnVerificationCompleteListener {
        void onVerificationComplete(boolean verified);
    }

    public void setOnVerificationCompleteListener(OnVerificationCompleteListener listener) {
        this.listener = listener;
    }

    public void setPhoneNumber(String phone) {
        this.phoneNumber = phone;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_step4_verify, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPhoneNumber = view.findViewById(R.id.tvPhoneNumber);
        tvVerified = view.findViewById(R.id.tvVerified);
        btnSendSms = view.findViewById(R.id.btnSendSms);
        btnVerify = view.findViewById(R.id.btnVerify);
        btnSkip = view.findViewById(R.id.btnSkip);
        etVerificationCode = view.findViewById(R.id.etVerificationCode);
        progressBar = view.findViewById(R.id.progressBar);

        if (phoneNumber != null) {
            tvPhoneNumber.setText(phoneNumber);
        }

        btnSendSms.setOnClickListener(v -> sendVerificationSms());

        btnVerify.setOnClickListener(v -> verifyCode());

        btnSkip.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVerificationComplete(false);
            }
        });
    }

    private void sendVerificationSms() {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(requireContext(), "No phone number provided", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendSms.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        PhoneAuthProvider.getInstance(FirebaseAuth.getInstance())
                .verifyPhoneNumber(phoneNumber, 60, java.util.concurrent.TimeUnit.SECONDS, requireActivity(),
                        new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                progressBar.setVisibility(View.GONE);
                                isVerified = true;
                                showVerifiedState();
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                progressBar.setVisibility(View.GONE);
                                btnSendSms.setVisibility(View.VISIBLE);
                                Toast.makeText(requireContext(), "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                progressBar.setVisibility(View.GONE);
                                FragmentStep4Verify.this.verificationId = verificationId;
                                etVerificationCode.setVisibility(View.VISIBLE);
                                btnVerify.setVisibility(View.VISIBLE);
                                Toast.makeText(requireContext(), "SMS sent!", Toast.LENGTH_SHORT).show();
                            }
                        });
    }

    private void verifyCode() {
        String code = etVerificationCode.getText() != null ? etVerificationCode.getText().toString().trim() : "";

        if (code.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter the verification code", Toast.LENGTH_SHORT).show();
            return;
        }

        if (verificationId == null) {
            Toast.makeText(requireContext(), "Please wait for SMS to be sent", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnVerify.setEnabled(false);

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        FirebaseAuth.getInstance().getCurrentUser().updatePhoneNumber(credential)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnVerify.setEnabled(true);

                    if (task.isSuccessful()) {
                        isVerified = true;
                        showVerifiedState();
                    } else {
                        Toast.makeText(requireContext(), "Verification failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showVerifiedState() {
        etVerificationCode.setVisibility(View.GONE);
        btnVerify.setVisibility(View.GONE);
        btnSendSms.setVisibility(View.GONE);
        tvVerified.setVisibility(View.VISIBLE);
        btnSkip.setText("Continue");

        btnSkip.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVerificationComplete(true);
            }
        });
    }
}
