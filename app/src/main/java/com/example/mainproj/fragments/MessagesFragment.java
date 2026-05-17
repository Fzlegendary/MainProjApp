package com.example.mainproj.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mainproj.R;
import com.example.mainproj.messaging.ChatActivity;
import com.example.mainproj.messaging.MessagingActivity;
import com.example.mainproj.messaging.friendRequest.IncomingRequestsActivity;
import com.google.android.material.button.MaterialButton;

public class MessagesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnOpenMessages = view.findViewById(R.id.btnOpenMessages);
        MaterialButton btnFriendRequests = view.findViewById(R.id.btnFriendRequests);

        btnOpenMessages.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), MessagingActivity.class));
        });

        btnFriendRequests.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), IncomingRequestsActivity.class));
        });
    }
}