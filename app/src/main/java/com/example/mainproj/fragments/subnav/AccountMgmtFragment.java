package com.example.mainproj.fragments.subnav;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mainproj.R;
import com.example.mainproj.AccountManagementActivity;
import com.google.android.material.button.MaterialButton;

public class AccountMgmtFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sub_account_mgmt, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnAccountManagement = view.findViewById(R.id.btnAccountManagement);
        btnAccountManagement.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AccountManagementActivity.class)));
    }
}