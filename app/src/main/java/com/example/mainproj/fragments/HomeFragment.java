package com.example.mainproj.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mainproj.R;
import com.example.mainproj.adapters.SubNavPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class HomeFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 subNavPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tabLayout);
        subNavPager = view.findViewById(R.id.subNavPager);

        setupSubNav();
    }

    private void setupSubNav() {
        SubNavPagerAdapter adapter = new SubNavPagerAdapter(requireActivity());
        subNavPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, subNavPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Gate Control");
                    break;
                case 1:
                    tab.setText("Server Settings");
                    break;
                case 2:
                    tab.setText("Account Mgmt");
                    break;
                case 3:
                    tab.setText("Blacklist");
                    break;
            }
        }).attach();
    }
}