package com.example.mainproj;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.mainproj.lists.FragmentBlacklist;
import com.example.mainproj.lists.FragmentWhitelist;

public class ListsPagerAdapter extends FragmentStateAdapter {

    public ListsPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new FragmentWhitelist();
        }
        return new FragmentBlacklist();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
