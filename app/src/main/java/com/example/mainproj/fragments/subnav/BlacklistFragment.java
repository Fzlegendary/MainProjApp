package com.example.mainproj.fragments.subnav;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mainproj.R;
import com.example.mainproj.SearchModule.ListsSearchViewModel;
import com.example.mainproj.SearchModule.suggestionsModule.SearchSuggestion;
import com.example.mainproj.SearchModule.suggestionsModule.SearchSuggestionAdapter;
import com.example.mainproj.lists.ListRequestManager;
import com.example.mainproj.lists.ListsPagerAdapter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BlacklistFragment extends Fragment {

    // ViewPager2 holds the inner Whitelist/Blacklist tabs
    private ViewPager2 viewPager;
    // TabLayout shows "Whitelist" / "Blacklist" tab indicators
    private TabLayout tabLayout;
    // Adapter manages the two list fragments inside the ViewPager
    private ListsPagerAdapter adapter;
    // API client for network operations (add/remove items)
    private ListRequestManager apiClient;

    // Filter button (the gear/filter icon in the UI)
    private ImageButton btnFilter;
    // Badge showing how many filters are active (e.g., "2")
    private TextView filterBadge;

    // The SearchView for typing queries (free text or filter commands)
    private SearchView searchView;
    // RecyclerView showing suggestion dropdown when typing "by:"
    private RecyclerView searchSuggestions;
    // Adapter that provides suggestion items to the RecyclerView
    private SearchSuggestionAdapter suggestionAdapter;
    // ViewModel holding the current search query as LiveData
    // Shared across all list fragments so they all filter together
    private ListsSearchViewModel searchViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout that contains SearchView + TabLayout + ViewPager2
        return inflater.inflate(R.layout.fragment_sub_blacklist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Base URL for the API (SSL bypass enabled for local dev)
        String baseUrl = "https://localhost:8080";
        // Initialize the API client for list operations
        apiClient = new ListRequestManager(baseUrl, true);

        // Bind UI elements from the layout
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.listViewPager);
        searchView = view.findViewById(R.id.searchView);
        searchSuggestions = view.findViewById(R.id.searchSuggestions);
        btnFilter = view.findViewById(R.id.btnFilter);
        filterBadge = view.findViewById(R.id.filterBadge);

        // Get the shared ViewModel — scoped to the Activity so it persists
        // across the Whitelist/Blacklist tabs. Both tabs observe this same query.
        searchViewModel = new ViewModelProvider(requireActivity()).get(ListsSearchViewModel.class);

        // Initialize the suggestion adapter — callback fires when user taps a suggestion
        suggestionAdapter = new SearchSuggestionAdapter(s -> {
            // Get whatever the user has typed so far
            String current = searchView.getQuery().toString();
            // Find where "by:" starts in the query
            int byIndex = current.toLowerCase().lastIndexOf("by:");
            if (byIndex >= 0) {
                // Keep everything before "by:" (including "by:")
                // Replace what comes after with the suggestion value
                String prefix = current.substring(0, byIndex + 3);
                searchView.setQuery(prefix + s.value + " ", false);
            } else {
                // No "by:" found — just append the suggestion as the new query
                searchView.setQuery(s.value + " ", false);
            }
        });

        // Set up the RecyclerView to show suggestions as a vertical list
        searchSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchSuggestions.setAdapter(suggestionAdapter);

        // Listen for SearchView text changes — submit on enter, update live as user types
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // User pressed Enter — update the ViewModel with the full query
                // This triggers the observer in both list fragments, filtering them
                searchViewModel.setSearchQuery(query);
                // Hide the suggestions dropdown when query is submitted
                searchSuggestions.setVisibility(View.GONE);
                // Update the filter badge count (shows how many filters are active)
                updateFilterBadge();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Live update as user types — set query in ViewModel
                searchViewModel.setSearchQuery(newText);
                // Recount active filters and update badge
                updateFilterBadge();

                // Known users that appear as suggestions after "by:"
                // In production this could come from a server or contacts list
                List<String> knownUsers = List.of("System");

                // Check if the query contains "by:" to trigger username suggestions
                if (newText.toLowerCase().contains("by:")) {
                    // Extract everything after "by:"
                    String afterBy = newText.substring(
                            newText.toLowerCase().lastIndexOf("by:") + 3
                    ).trim();

                    // Build a list of suggestions matching what the user typed
                    List<SearchSuggestion> suggestions = new ArrayList<>();
                    for (String s : knownUsers) {
                        // Case-insensitive prefix match
                        if (s.toLowerCase().startsWith(afterBy.toLowerCase())) {
                            suggestions.add(new SearchSuggestion(s, s));
                        }
                    }

                    // Show dropdown if there are suggestions, hide otherwise
                    suggestionAdapter.setItems(suggestions);
                    searchSuggestions.setVisibility(
                            suggestions.isEmpty() ? View.GONE : View.VISIBLE
                    );

                    // Hide suggestions once the user has fully typed a known username
                    // (so the dropdown disappears when the suggestion is "complete")
                    if (knownUsers.contains(afterBy)) {
                        searchSuggestions.setVisibility(View.GONE);
                    }
                } else {
                    // No "by:" in query — hide suggestions
                    searchSuggestions.setVisibility(View.GONE);
                }
                return true;
            }
        });

        // Set up the inner ViewPager2 with Whitelist / Blacklist tabs
        setupViewPager();

        // Set up the filter button to open the popup menu
        // Pass 'view' as parameter so the method can find child views within it
        setupFilterButton(view);
    }

    private void setupViewPager() {
        // Create adapter with the API client (used for network add/remove ops)
        adapter = new ListsPagerAdapter(requireActivity(), apiClient);
        // Attach adapter to the ViewPager — creates FragmentWhitelist (pos 0)
        // and FragmentBlacklist (pos 1) via createFragment()
        viewPager.setAdapter(adapter);

        // Connect TabLayout to ViewPager2 so swiping changes the tab indicator
        // and tapping a tab scrolls to that page
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            // Set tab titles — Tab 0 = Whitelist, Tab 1 = Blacklist
            if (position == 0) {
                tab.setText("Whitelist");
            } else {
                tab.setText("Blacklist");
            }
        }).attach();

        // Observe the search query in the ViewModel — whenever it changes,
        // filter both fragments (Whitelist and Blacklist) simultaneously.
        // This ensures both tabs stay in sync with the same search/filter.
        searchViewModel.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            // Iterate over both list fragments in the ViewPager
            for (int i = 0; i < adapter.getItemCount(); i++) {
                // Get the fragment at position (Whitelist or Blacklist)
                com.example.mainproj.lists.FragmentBaseList fragment = adapter.getFragmentAt(i);
                if (fragment != null) {
                    // Apply the search/filter logic:
                    // - Extract free text, by:user, expires/added dates from query
                    // - Return a filtered list matching all criteria
                    // - updateList() updates the RecyclerView adapter
                    fragment.updateList(
                            com.example.mainproj.SearchModule.ListFilter.apply(
                                    fragment.getFullList(), query
                            )
                    );
                }
            }
        });
    }

    private void setupFilterButton(View view) {
        // Bind the filter ImageButton from the layout
        btnFilter = view.findViewById(R.id.btnFilter);
        // Bind the badge TextView (shows count of active filters)
        filterBadge = view.findViewById(R.id.filterBadge);

        // Open the filter popup menu when the filter button is clicked
        btnFilter.setOnClickListener(this::showFilterMenu); //this:: takes the current (this) view and passes it to showFilterMenu
    }

    /**
     * Displays a PopupMenu with all available filter options.
     * Each option appends a filter token to the SearchView query.
     * @param anchor The View to anchor the popup menu to (the filter button)
     */
    private void showFilterMenu(View anchor) {
        // Create a PopupMenu anchored to the filter button
        androidx.appcompat.widget.PopupMenu popup =
                new androidx.appcompat.widget.PopupMenu(requireContext(), anchor);
        // Inflate the menu XML that defines the 8 filter options
        popup.getMenuInflater().inflate(R.menu.menu_filter, popup.getMenu());

        // Handle clicks on each menu item
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            // menu_by_user → opens dialog to type a username
            if (id == R.id.menu_by_user) {
                showByUserDialog();
                return true;
            // menu_expires_before → date picker → append "expiresBefore:DATE"
            } else if (id == R.id.menu_expires_before) {
                showDateFilterDialog("expiresBefore:");
                return true;
            // menu_expires_after → date picker → append "expiresAfter:DATE"
            } else if (id == R.id.menu_expires_after) {
                showDateFilterDialog("expiresAfter:");
                return true;
            // menu_expires_on → date picker → append "expires:DATE"
            } else if (id == R.id.menu_expires_on) {
                showDateFilterDialog("expires:");
                return true;
            // menu_added_before → date picker → append "addedBefore:DATE"
            } else if (id == R.id.menu_added_before) {
                showDateFilterDialog("addedBefore:");
                return true;
            // menu_added_after → date picker → append "addedAfter:DATE"
            } else if (id == R.id.menu_added_after) {
                showDateFilterDialog("addedAfter:");
                return true;
            // menu_added_on → date picker → append "added:DATE"
            } else if (id == R.id.menu_added_on) {
                showDateFilterDialog("added:");
                return true;
            // menu_clear_filters → clears the entire search query
            } else if (id == R.id.menu_clear_filters) {
                clearFilters();
                return true;
            }
            return false;
        });
        popup.show();
    }

    /**
     * Shows a dialog asking the user to type a username.
     * The username is appended as "by:USERNAME" to the search query.
     */
    private void showByUserDialog() {
        // Inflate the dialog layout (TextInputEditText with hint "Username")
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_input, null);
        // Bind the text field where the user will type the username
        TextInputEditText etValue = dialogView.findViewById(R.id.etFilterValue);

        new AlertDialog.Builder(requireContext())
                .setTitle("Filter by User")
                .setView(dialogView)
                // "Apply" button — appends the filter to the current search query
                .setPositiveButton("Apply", (dialog, which) -> {
                    String value = etValue.getText() != null
                            ? etValue.getText().toString().trim()
                            : "";
                    // Only add filter if user typed something
                    if (!value.isEmpty()) {
                        appendFilter("by:" + value);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a DatePickerDialog and appends a date-based filter to the search query.
     * @param prefix The filter token prefix, e.g. "expiresBefore:", "added:"
     */
    private void showDateFilterDialog(String prefix) {
        // Default to today's date in the date picker
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(
                requireContext(),
                // Callback when user selects a date
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    // Format date as dd/MM/yyyy (matches ListFilter parsing expectations)
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String dateStr = sdf.format(selected.getTime());
                    // Append filter token + date to the search query
                    // e.g., "expiresBefore:15/05/2026"
                    appendFilter(prefix + dateStr);
                },
                // Default displayed date = today
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    /**
     * Appends a filter token to the current search query.
     * If the query is empty, the filter becomes the new query.
     * Otherwise, filters are comma-separated.
     * @param filter The filter token to append (e.g., "by:John", "expiresBefore:15/05/2026")
     */
    private void appendFilter(String filter) {
        // Get the current search query from the ViewModel
        String current = searchViewModel.getSearchQuery().getValue();
        if (current == null) current = "";
        current = current.trim();

        if (current.isEmpty()) {
            // No existing query — start with just the filter
            searchView.setQuery(filter, false);
        } else {
            // Append comma-separated: "existing query, new_filter"
            searchView.setQuery(current + ", " + filter, false);
        }
        // Also update the ViewModel directly (SearchView.setQuery triggers listener
        // but we update it again to ensure consistency)
        searchViewModel.setSearchQuery(searchView.getQuery().toString());
        // Update the badge to reflect the new active filter count
        updateFilterBadge();
    }

    /**
     * Clears the entire search query, removing all text and filters.
     */
    private void clearFilters() {
        // Clear the SearchView text
        searchView.setQuery("", false);
        // Reset the ViewModel query to empty string
        searchViewModel.setSearchQuery("");
        // Hide the filter badge (no active filters)
        updateFilterBadge();
    }

    /**
     * Updates the filter badge visibility and text based on the current query.
     * The badge shows the count of active filter tokens (not just text).
     * For "by:" filters, only show badge if a username is typed (length > 3).
     */
    private void updateFilterBadge() {
        String query = searchViewModel.getSearchQuery().getValue();

        // If query is null or empty, hide the badge
        if (query == null || query.trim().isEmpty()) {
            filterBadge.setVisibility(View.GONE);
            return;
        }

        // Split query by commas — each part may be a filter
        String[] parts = query.split(",");
        int filterCount = 0;

        for (String part : parts) {
            String trimmed = part.trim();
            // Count as a filter if:
            // - Contains ":" (a key:value pair) and is not a bare "by:"
            // - OR is "by:USERNAME" with actual username (length > 3 after "by:")
            boolean isFilter = (trimmed.contains(":") && !trimmed.startsWith("by:"))
                    || (trimmed.startsWith("by:") && trimmed.length() > 3);

            if (isFilter) {
                filterCount++;
            }
        }

        if (filterCount > 0) {
            // Show badge with filter count
            filterBadge.setText(String.valueOf(filterCount));
            filterBadge.setVisibility(View.VISIBLE);
        } else {
            // No filters — hide the badge (plain text search doesn't count)
            filterBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}