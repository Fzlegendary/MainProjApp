package com.example.mainproj.lists;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.HashMap;
import java.util.Map;

public class ListsPagerAdapter extends FragmentStateAdapter {

    private final Map<Integer, FragmentBaseList> fragmentMap = new HashMap<>();
    private final ListRequestManager apiClient; // store the client

    // Updated constructor – now takes two arguments
    public ListsPagerAdapter(@NonNull FragmentActivity activity, ListRequestManager apiClient) {
        super(activity);
        this.apiClient = apiClient;
    }

    @NonNull
    @Override
    public FragmentBaseList createFragment(int position) {
        FragmentBaseList fragment;
        if (position == 0) {
            fragment = new FragmentWhitelist();
        } else {
            fragment = new FragmentBlacklist();
        }
        // Pass the apiClient to the fragment (requires a setter in FragmentBaseList)
        fragment.setApiClient(apiClient);
        fragmentMap.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public FragmentBaseList getFragmentAt(int position) {
        return fragmentMap.get(position);
    }
}