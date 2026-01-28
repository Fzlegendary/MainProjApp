package com.example.mainproj.lists;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;

import com.example.mainproj.R;
import com.example.mainproj.SearchModule.ListFilter;
import com.example.mainproj.SearchModule.ListsSearchViewModel;
import com.example.mainproj.shared_lists_adapter_placeholder.ListAdapter;
import com.example.mainproj.shared_lists_adapter_placeholder.PlaceholderContent;

import java.util.ArrayList;
import java.util.List;

public class FragmentBlacklist extends FragmentBaseList {

    private ListAdapter adapter;
    private List<PlaceholderContent.PlaceholderItem> fullList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list, container, false);

        RecyclerView rv = view.findViewById(R.id.recyclerViewList);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        fullList = new ArrayList<>(PlaceholderContent.BLACKLIST_ITEMS); // blacklist items
        adapter = new ListAdapter(fullList);
        rv.setAdapter(adapter);

        // Observe search query
        ListsSearchViewModel vm =
                new ViewModelProvider(requireActivity()).get(ListsSearchViewModel.class);
        vm.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            adapter.update(ListFilter.apply(fullList, query));
        });

        return view;
    }

    @Override
    public List<PlaceholderContent.PlaceholderItem> getFullList() {
        return fullList;
    }

    @Override
    public void updateList(List<PlaceholderContent.PlaceholderItem> filtered) {
        adapter.update(filtered);
    }
}
