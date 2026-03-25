package com.example.mainproj.userSystem;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FragmentStep2Username extends Fragment {

    private TextInputEditText etUsername, etNickname;
    private MaterialButton btnNext;
    private TextView tvUsernameError;

    private boolean isUsernameValid = false;

    private OnUsernameCompleteListener listener;

    public interface OnUsernameCompleteListener {
        void onUsernameNext(String username, String nickname);
    }

    public void setOnUsernameCompleteListener(OnUsernameCompleteListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_step2_username, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etUsername = view.findViewById(R.id.etUsername);
        etNickname = view.findViewById(R.id.etNickname);
        btnNext = view.findViewById(R.id.btnNext);
        tvUsernameError = view.findViewById(R.id.tvUsernameError);

        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateUsername(s.toString().trim());
            }
        });

        btnNext.setOnClickListener(v -> {
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            String nickname = etNickname.getText() != null ? etNickname.getText().toString().trim() : "";

            if (TextUtils.isEmpty(username)) {
                Toast.makeText(requireContext(), "Username is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isUsernameValid) {
                Toast.makeText(requireContext(), "Please enter a valid username", Toast.LENGTH_SHORT).show();
                return;
            }

            if (listener != null) {
                listener.onUsernameNext(username, nickname);
            }
        });
    }

    private void validateUsername(String username) {
        if (TextUtils.isEmpty(username)) {
            tvUsernameError.setVisibility(View.GONE);
            isUsernameValid = false;
            return;
        }

        if (username.length() < 3) {
            tvUsernameError.setText("Username must be at least 3 characters");
            tvUsernameError.setVisibility(View.VISIBLE);
            isUsernameValid = false;
            return;
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            tvUsernameError.setText("Username can only contain letters, numbers, and underscores");
            tvUsernameError.setVisibility(View.VISIBLE);
            isUsernameValid = false;
            return;
        }

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("usernames");
        usersRef.orderByKey().equalTo(username.toLowerCase()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvUsernameError.setText("Username is already taken");
                    tvUsernameError.setVisibility(View.VISIBLE);
                    isUsernameValid = false;
                } else {
                    tvUsernameError.setVisibility(View.GONE);
                    isUsernameValid = true;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvUsernameError.setVisibility(View.GONE);
                isUsernameValid = true;
            }
        });
    }
}
