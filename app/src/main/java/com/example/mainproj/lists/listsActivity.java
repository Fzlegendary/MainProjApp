package com.example.mainproj.lists;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.mainproj.lists.ListRequestManager;
import com.example.mainproj.shared_lists_adapter_placeholder.PlaceholderContent;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

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
    private ListRequestManager apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lists);

        // Initialize API client with localhost (works with adb reverse)
        String baseUrl = "https://localhost:8080";
        apiClient = new ListRequestManager(baseUrl, true); // true = trust self‑signed cert (dev only)

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
                List<String> knownUsers = List.of("System");
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

        // Pass apiClient to the adapter
        adapter = new ListsPagerAdapter(this, apiClient);
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

    /**
     * Called by fragments to add an item.
     */
    public void addItem(String listType, PlaceholderContent.PlaceholderItem item, int position) {
        String userName = getCurrentUserName();
        apiClient.addItem(listType, item, userName, new ListRequestManager.ListCallback() {
            @Override
            public void onSuccess(String commandId, String itemId) {
                item.id = itemId; // set the server-generated id
                runOnUiThread(() -> {
                    List<PlaceholderContent.PlaceholderItem> targetList =
                            listType.equals("whitelist") ? PlaceholderContent.ITEMS : PlaceholderContent.BLACKLIST_ITEMS;
                    targetList.add(item);
                    refreshFragment(listType);
                    Toast.makeText(listsActivity.this, "Item added", Toast.LENGTH_SHORT).show();

                    // Send a log to the server's /lists/log endpoint
                    sendItemLog(listType, item, userName);
                });
            }

            @Override
            public void onRejected(String reason, String commandId) {
                runOnUiThread(() -> Toast.makeText(listsActivity.this,
                        "Rejected: " + reason, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(listsActivity.this,
                        "Error: " + errorMessage, Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Called by fragments to remove an item.
     */
    public void removeItem(String listType, PlaceholderContent.PlaceholderItem item, int position) {
        if (item.id == null) {
            Toast.makeText(this, "Item not synced", Toast.LENGTH_SHORT).show();
            return;
        }
        String userName = getCurrentUserName();
        apiClient.removeItem(listType, item.id, userName, new ListRequestManager.ListCallback() {
            @Override
            public void onSuccess(String commandId, String itemId) {
                runOnUiThread(() -> {
                    List<PlaceholderContent.PlaceholderItem> targetList =
                            listType.equals("whitelist") ? PlaceholderContent.ITEMS : PlaceholderContent.BLACKLIST_ITEMS;
                    targetList.remove(item);
                    refreshFragment(listType);
                    Toast.makeText(listsActivity.this, "Item removed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onRejected(String reason, String commandId) {
                runOnUiThread(() -> Toast.makeText(listsActivity.this,
                        "Rejected: " + reason, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(listsActivity.this,
                        "Error: " + errorMessage, Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Send a log entry to the server's /lists/log endpoint.
     */
    private void sendItemLog(String listType, PlaceholderContent.PlaceholderItem item, String userName) {
        Log.d("ItemLog", "Preparing to send log for item: " + item.name);
        new Thread(() -> {
            HttpsURLConnection conn = null;
            try {
                String baseUrl = "https://localhost:8080";
                URL url = new URL(baseUrl + "/lists/log");
                conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                JSONObject payload = new JSONObject();
                payload.put("listType", listType);
                payload.put("userName", userName);
                String commandId = UUID.randomUUID().toString();
                payload.put("commandId", commandId);

                JSONObject itemJson = new JSONObject();
                itemJson.put("name", item.name);
                itemJson.put("reason", item.reason);
                itemJson.put("expiresAt", item.timestampExpiresAt);
                payload.put("item", itemJson);

                Log.d("ItemLog", "Sending payload: " + payload.toString());

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(payload.toString());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("ItemLog", "Response code: " + responseCode);

                if (responseCode == 200) {
                    Log.d("ItemLog", "Log sent successfully for item: " + item.name);
                } else {
                    // Read error response
                    String errorResponse = "";
                    if (conn.getErrorStream() != null) {
                        java.util.Scanner s = new java.util.Scanner(conn.getErrorStream()).useDelimiter("\\A");
                        errorResponse = s.hasNext() ? s.next() : "";
                    }
                    Log.e("ItemLog", "Failed to send log. Response code: " + responseCode + " - " + errorResponse);
                }
            } catch (Exception e) {
                Log.e("ItemLog", "Error sending log", e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
    private void refreshFragment(String listType) {
        int index = listType.equals("whitelist") ? 0 : 1;
        FragmentBaseList fragment = adapter.getFragmentAt(index);
        if (fragment != null) {
            String currentQuery = searchViewModel.getSearchQuery().getValue();
            List<PlaceholderContent.PlaceholderItem> filtered =
                    ListFilter.apply(fragment.getFullList(), currentQuery);
            fragment.updateList(filtered);
        }
    }

    private String getCurrentUserName() {
        // TODO: return real user name from SharedPreferences or FirebaseAuth
        return "Admin";
    }
}