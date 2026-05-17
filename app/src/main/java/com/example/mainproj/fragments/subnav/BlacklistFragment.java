package com.example.mainproj.fragments.subnav;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mainproj.R;
import com.example.mainproj.SearchModule.ListsSearchViewModel;
import com.example.mainproj.SearchModule.suggestionsModule.SearchSuggestion;
import com.example.mainproj.SearchModule.suggestionsModule.SearchSuggestionAdapter;
import com.example.mainproj.lists.ListRequestManager;
import com.example.mainproj.lists.ListsPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class BlacklistFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ListsPagerAdapter adapter;
    private ListRequestManager apiClient;

    private ImageButton btnFilter;
    private TextView filterBadge;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sub_blacklist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String baseUrl = "https://localhost:8080";
        apiClient = new ListRequestManager(baseUrl, true);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.listViewPager);

        setupViewPager();
        setupFilterButton(view);
    }

    private void setupViewPager() {
        adapter = new ListsPagerAdapter(requireActivity(), apiClient);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Whitelist");
            } else {
                tab.setText("Blacklist");
            }
        }).attach();
    }

    private void setupFilterButton(View view) {
        btnFilter = view.findViewById(R.id.btnFilter);
        filterBadge = view.findViewById(R.id.filterBadge);

        btnFilter.setOnClickListener(v -> {
            // Toggle filter - simplified for now
            if (filterBadge.getVisibility() == View.VISIBLE) {
                filterBadge.setVisibility(View.GONE);
            } else {
                filterBadge.setVisibility(View.VISIBLE);
                filterBadge.setText("0");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}