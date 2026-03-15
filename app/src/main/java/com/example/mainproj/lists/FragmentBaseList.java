package com.example.mainproj.lists;

import androidx.fragment.app.Fragment;

import com.example.mainproj.shared_lists_adapter_placeholder.PlaceholderContent;

import java.util.List;

public abstract class FragmentBaseList extends Fragment {

    protected ListRequestManager apiClient;  // Add this field

    /**
     * Set the API client for this fragment (called by the adapter).
     */
    public void setApiClient(ListRequestManager apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Returns the full list (original items)
     */
    public abstract List<PlaceholderContent.PlaceholderItem> getFullList();

    /**
     * Updates the list shown in RecyclerView after filtering
     */
    public abstract void updateList(List<PlaceholderContent.PlaceholderItem> filtered);
}