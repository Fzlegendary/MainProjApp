package com.example.mainproj.userSystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mainproj.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class FragmentStep1Phone extends Fragment {

    private TextInputEditText etPhone;
    private MaterialButton btnNext, btnSkip;

    private OnPhoneCompleteListener listener;

    public interface OnPhoneCompleteListener {
        void onPhoneNext(String phone);
        void onPhoneSkip();
    }

    public void setOnPhoneCompleteListener(OnPhoneCompleteListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_step1_phone, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPhone = view.findViewById(R.id.etPhone);
        btnNext = view.findViewById(R.id.btnNext);
        btnSkip = view.findViewById(R.id.btnSkip);

        btnNext.setOnClickListener(v -> {
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            
            if (!phone.isEmpty() && !phone.startsWith("+")) {
                Toast.makeText(requireContext(), "Please include country code (e.g., +1)", Toast.LENGTH_SHORT).show();
                return;
            }

            if (listener != null) {
                listener.onPhoneNext(phone);
            }
        });

        btnSkip.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPhoneSkip();
            }
        });
    }
}
