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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mainproj.ListsPagerAdapter;
import com.example.mainproj.R;
import com.example.mainproj.SearchModule.ListsSearchViewModel;
import com.example.mainproj.SearchModule.SearchSuggestion;
import com.example.mainproj.SearchModule.SearchSuggestionAdapter;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lists);

        // Edge-to-edge padding
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

        // Shared ViewModel
        ListsSearchViewModel searchViewModel =
                new ViewModelProvider(this).get(ListsSearchViewModel.class);

        // Setup suggestion RecyclerView
        suggestionAdapter = new SearchSuggestionAdapter(s -> {
            // Replace the partial "by:" text with selected suggestion
            String current = searchView.getQuery().toString();
            int byIndex = current.toLowerCase().lastIndexOf("by:");
            if (byIndex >= 0) {
                String prefix = current.substring(0, byIndex + 3); // include "by:"
                searchView.setQuery(prefix + s.value + " ", false);
            } else {
                searchView.setQuery(s.value + " ", false);
            }
        });
        searchSuggestions.setLayoutManager(new LinearLayoutManager(this));
        searchSuggestions.setAdapter(suggestionAdapter);

        // Show suggestions dynamically
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

                // Suggest "by:" usernames
                if (newText.toLowerCase().contains("by:")) {
                    String afterBy = newText.substring(newText.toLowerCase().lastIndexOf("by:") + 3).trim();
                    List<SearchSuggestion> suggestions = new ArrayList<>();
                    for (String s : List.of("System", "S1", "Admin")) {
                        if (s.toLowerCase().startsWith(afterBy.toLowerCase())) {
                            suggestions.add(new SearchSuggestion(s, s));
                        }
                    }
                    suggestionAdapter.setItems(suggestions);
                    searchSuggestions.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
                } else {
                    searchSuggestions.setVisibility(View.GONE);
                }

                return true;
            }
        });

        // Set up ViewPager adapter
        ListsPagerAdapter adapter = new ListsPagerAdapter(this);
        viewPager.setAdapter(adapter);
        int pageCount = adapter.getItemCount();

        // Add bottom buttons dynamically
        int indicatorColor = 0xFF6200EE;
        for (int i = 0; i < pageCount; i++) {
            Button btn = new Button(this);
            btn.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            btn.setBackgroundColor(0x00000000); // transparent
            btn.setText(pageTitles[i]);
            btn.setTextColor(indicatorColor);
            final int index = i;
            btn.setOnClickListener(v -> viewPager.setCurrentItem(index, true));
            indicatorButtons.addView(btn);
        }

        fragmentTitle.setText(pageTitles[0]);

        // Update slider on scroll
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
    }
}
