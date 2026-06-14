package com.example.mainproj.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.mainproj.fragments.IncomingRequestsFragment;
import com.example.mainproj.fragments.OutgoingRequestsFragment;

public class FriendRequestsPagerAdapter extends FragmentStateAdapter {

    public FriendRequestsPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new IncomingRequestsFragment();
        } else {
            return new OutgoingRequestsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}