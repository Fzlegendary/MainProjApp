package com.example.mainproj.lists;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.example.mainproj.SearchModule.ListFilter;
import com.example.mainproj.SearchModule.ListsSearchViewModel;
import com.example.mainproj.SearchModule.suggestionsModule.SearchSuggestion;
import com.example.mainproj.SearchModule.suggestionsModule.SearchSuggestionAdapter;
import com.example.mainproj.shared_lists_adapter_placeholder.PlaceholderContent;

import java.util.ArrayList;
import java.util.List;

public class listsActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private View indicatorSlider, indicatorBar;
    private LinearLayout indicatorButtons;
    private TextView fragmentTitle;

    private final String[] pageTitles = {"Whitelist", "Blacklist"};

    private RecyclerView searchSuggestions;
    private SearchSuggestionAdapter suggestionAdapter;

    private ListsPagerAdapter adapter;
    private ListsSearchViewModel searchViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lists);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewPager = findViewById(R.id.viewPager);
        indicatorSlider = findViewById(R.id.indicatorSlider);
        indicatorBar = findViewById(R.id.indicatorBar);
        indicatorButtons = findViewById(R.id.indicatorButtons);
        fragmentTitle = findViewById(R.id.fragmentTitle);
        SearchView searchView = findViewById(R.id.searchView);
        searchSuggestions = findViewById(R.id.searchSuggestions);

        searchViewModel = new ViewModelProvider(this).get(ListsSearchViewModel.class);

        suggestionAdapter = new SearchSuggestionAdapter(s -> {
            String current = searchView.getQuery().toString();
            int byIndex = current.toLowerCase().lastIndexOf("by:");
            if (byIndex >= 0) {
                String prefix = current.substring(0, byIndex + 3);
                searchView.setQuery(prefix + s.value + " ", false);
            } else {
                searchView.setQuery(s.value + " ", false);
            }
        });
        searchSuggestions.setLayoutManager(new LinearLayoutManager(this));
        searchSuggestions.setAdapter(suggestionAdapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchViewModel.setSearchQuery(query);
                searchSuggestions.setVisibility(View.GONE);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchViewModel.setSearchQuery(newText);

                List<String> knownUsers = List.of("System"); // Firebase admins placeholder
                if (newText.toLowerCase().contains("by:")) {
                    String afterBy = newText.substring(newText.toLowerCase().lastIndexOf("by:") + 3).trim();
                    List<SearchSuggestion> suggestions = new ArrayList<>();
                    for (String s : knownUsers) {
                        if (s.toLowerCase().startsWith(afterBy.toLowerCase())) {
                            suggestions.add(new SearchSuggestion(s, s));
                        }
                    }
                    suggestionAdapter.setItems(suggestions);
                    searchSuggestions.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
                    if (knownUsers.contains(afterBy)) searchSuggestions.setVisibility(View.GONE);
                } else {
                    searchSuggestions.setVisibility(View.GONE);
                }

                return true;
            }
        });

        adapter = new ListsPagerAdapter(this);
        viewPager.setAdapter(adapter);
        int pageCount = adapter.getItemCount();

        int indicatorColor = 0xFF6200EE;
        for (int i = 0; i < pageCount; i++) {
            Button btn = new Button(this);
            btn.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            btn.setBackgroundColor(0x00000000);
            btn.setText(pageTitles[i]);
            btn.setTextColor(indicatorColor);
            final int index = i;
            btn.setOnClickListener(v -> viewPager.setCurrentItem(index, true));
            indicatorButtons.addView(btn);
        }

        fragmentTitle.setText(pageTitles[0]);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float offset, int px) {
                int sliderWidth = indicatorBar.getWidth() / pageCount;
                indicatorSlider.getLayoutParams().width = sliderWidth;
                indicatorSlider.setTranslationX((position + offset) * sliderWidth);
                indicatorSlider.requestLayout();
            }

            @Override
            public void onPageSelected(int position) {
                fragmentTitle.setText(pageTitles[position]);
            }
        });

        // Search query observer: filter all fragments
        searchViewModel.getSearchQuery().observe(this, query -> {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                FragmentBaseList fragment = adapter.getFragmentAt(i);
                if (fragment != null) {
                    fragment.updateList(ListFilter.apply(fragment.getFullList(), query));
                }
            }
        });
    }
}
