package com.example.mainproj.SearchModule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ListsSearchViewModel extends ViewModel {

    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query == null ? "" : query);
    }
}
