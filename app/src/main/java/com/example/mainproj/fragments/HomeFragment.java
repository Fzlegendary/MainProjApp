package com.example.mainproj.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mainproj.AccountManagementActivity;
import com.example.mainproj.R;
import com.example.mainproj.ServerSettingsActivity;
import com.example.mainproj.gatesMain;
import com.example.mainproj.lists.listsActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

public class HomeFragment extends Fragment {

    private TabLayout tabLayout;
    private View gateControlView, serverSettingsView, accountManagementView, blacklistView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tabLayout);

        gateControlView = view.findViewById(R.id.gateControlContent);
        serverSettingsView = view.findViewById(R.id.serverSettingsContent);
        accountManagementView = view.findViewById(R.id.accountManagementContent);
        blacklistView = view.findViewById(R.id.blacklistContent);

        setupTabs();
        setupButtonClicks();

        showTabContent(0);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Gate Control"));
        tabLayout.addTab(tabLayout.newTab().setText("Server Settings"));
        tabLayout.addTab(tabLayout.newTab().setText("Account Mgmt"));
        tabLayout.addTab(tabLayout.newTab().setText("Blacklist"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTabContent(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupButtonClicks() {
        MaterialButton btnGateControl = gateControlView.findViewById(R.id.btnGateControl);
        MaterialButton btnServerSettings = serverSettingsView.findViewById(R.id.btnServerSettings);
        MaterialButton btnAccountManagement = accountManagementView.findViewById(R.id.btnAccountManagement);
        MaterialButton btnBlacklist = blacklistView.findViewById(R.id.btnBlacklist);

        btnGateControl.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), gatesMain.class)));

        btnServerSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ServerSettingsActivity.class)));

        btnAccountManagement.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AccountManagementActivity.class)));

        btnBlacklist.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), listsActivity.class)));
    }

    private void showTabContent(int position) {
        gateControlView.setVisibility(View.GONE);
        serverSettingsView.setVisibility(View.GONE);
        accountManagementView.setVisibility(View.GONE);
        blacklistView.setVisibility(View.GONE);

        switch (position) {
            case 0:
                gateControlView.setVisibility(View.VISIBLE);
                break;
            case 1:
                serverSettingsView.setVisibility(View.VISIBLE);
                break;
            case 2:
                accountManagementView.setVisibility(View.VISIBLE);
                break;
            case 3:
                blacklistView.setVisibility(View.VISIBLE);
                break;
        }
    }
}