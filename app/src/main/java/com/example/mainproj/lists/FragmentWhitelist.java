package com.example.mainproj.lists;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
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

public class FragmentWhitelist extends FragmentBaseList {

    private ListAdapter adapter;
    private List<PlaceholderContent.PlaceholderItem> fullList = new ArrayList<>();
    private static final String LIST_TYPE = "whitelist";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list, container, false);

        RecyclerView rv = view.findViewById(R.id.recyclerViewList);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        fullList = new ArrayList<>(PlaceholderContent.ITEMS);
        adapter = new ListAdapter(fullList);
        adapter.setOnDeleteClickListener(this::showDeleteConfirmation);
        rv.setAdapter(adapter);

        ListsSearchViewModel vm =
                new ViewModelProvider(requireActivity()).get(ListsSearchViewModel.class);
        vm.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            adapter.update(ListFilter.apply(fullList, query));
        });

        return view;
    }

    private void showDeleteConfirmation(int position, PlaceholderContent.PlaceholderItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove " + item.name + "?")
                .setMessage("This will remove the item from the whitelist.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    fullList.remove(item);
                    adapter.removeItem(position);
                    if (apiClient != null && item.id != null) {
                        ((listsActivity) requireActivity()).removeItem(LIST_TYPE, item, position);
                    } else if (item.id == null) {
                        Toast.makeText(requireContext(), "Item removed locally", Toast.LENGTH_SHORT).show();
                    }
                    refreshFilteredList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshFilteredList() {
        ListsSearchViewModel vm =
                new ViewModelProvider(requireActivity()).get(ListsSearchViewModel.class);
        String query = vm.getSearchQuery().getValue();
        if (query != null && !query.isEmpty()) {
            adapter.update(ListFilter.apply(fullList, query));
        }
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
