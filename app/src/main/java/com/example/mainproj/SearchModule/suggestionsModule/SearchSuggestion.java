package com.example.mainproj.SearchModule.suggestionsModule;

public class SearchSuggestion {
    public final String display; // what shows to user
    public final String value;   // value to insert

    public SearchSuggestion(String display, String value) {
        this.display = display;
        this.value = value;
    }
}
