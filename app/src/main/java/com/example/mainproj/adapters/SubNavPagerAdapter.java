package com.example.mainproj.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.mainproj.fragments.subnav.AccountMgmtFragment;
import com.example.mainproj.fragments.subnav.BlacklistFragment;
import com.example.mainproj.fragments.subnav.GateControlFragment;
import com.example.mainproj.fragments.subnav.ServerSettingsFragment;

public class SubNavPagerAdapter extends FragmentStateAdapter {

    public SubNavPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new GateControlFragment();
            case 1:
                return new ServerSettingsFragment();
            case 2:
                return new AccountMgmtFragment();
            case 3:
                return new BlacklistFragment();
            default:
                return new GateControlFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}