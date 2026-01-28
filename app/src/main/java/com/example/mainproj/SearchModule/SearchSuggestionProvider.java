package com.example.mainproj.SearchModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchSuggestionProvider {

    private static final List<String> BY_VALUES =
            Arrays.asList("system", "s1", "s2", "admin");

    public static List<SearchSuggestion> getSuggestions(String input) {
        List<SearchSuggestion> out = new ArrayList<>();

        if (input == null) return out;

        String lastToken = getLastToken(input.toLowerCase());

        // by: suggestions
        if (lastToken.startsWith("by:")) {
            String typed = lastToken.substring(3);

            for (String v : BY_VALUES) {
                if (v.startsWith(typed)) {
                    out.add(new SearchSuggestion(
                            "By: " + v,
                            "by:" + v
                    ));
                }
            }
        }

        // modifier suggestions (optional but recommended)
        if ("".equals(lastToken) || lastToken.length() < 3) {
            out.add(new SearchSuggestion("expires:", "expires:"));
            out.add(new SearchSuggestion("expiresBefore:", "expiresBefore:"));
            out.add(new SearchSuggestion("expiresAfter:", "expiresAfter:"));
            out.add(new SearchSuggestion("added:", "added:"));
            out.add(new SearchSuggestion("addedBefore:", "addedBefore:"));
            out.add(new SearchSuggestion("addedAfter:", "addedAfter:"));
        }

        return out;
    }

    private static String getLastToken(String input) {
        int idx = input.lastIndexOf(' ');
        return idx == -1 ? input : input.substring(idx + 1);
    }
}
