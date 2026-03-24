package com.example.mainproj.lists;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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

import com.example.mainproj.BaseActivity;
import com.example.mainproj.R;
import com.example.mainproj.SearchModule.ListFilter;
import com.example.mainproj.SearchModule.ListsSearchViewModel;
import com.example.mainproj.SearchModule.suggestionsModule.SearchSuggestion;
import com.example.mainproj.SearchModule.suggestionsModule.SearchSuggestionAdapter;
import com.example.mainproj.lists.ListRequestManager;
import com.example.mainproj.shared_lists_adapter_placeholder.PlaceholderContent;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class listsActivity extends BaseActivity {

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

    private ImageButton btnFilter;
    private TextView filterBadge;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lists);

        String baseUrl = "https://localhost:8080";
        apiClient = new ListRequestManager(baseUrl, true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        viewPager = findViewById(R.id.viewPager);
        indicatorSlider = findViewById(R.id.indicatorSlider);
        indicatorBar = findViewById(R.id.indicatorBar);
        indicatorButtons = findViewById(R.id.indicatorButtons);
        fragmentTitle = findViewById(R.id.fragmentTitle);
        searchView = findViewById(R.id.searchView);
        searchSuggestions = findViewById(R.id.searchSuggestions);
        btnFilter = findViewById(R.id.btnFilter);
        filterBadge = findViewById(R.id.filterBadge);

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
                updateFilterBadge();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchViewModel.setSearchQuery(newText);
                updateFilterBadge();
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

        btnFilter.setOnClickListener(v -> showFilterMenu(v));

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

        searchViewModel.getSearchQuery().observe(this, query -> {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                FragmentBaseList fragment = adapter.getFragmentAt(i);
                if (fragment != null) {
                    fragment.updateList(ListFilter.apply(fragment.getFullList(), query));
                }
            }
        });
    }

    private void showFilterMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_filter, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_by_user) {
                showByUserDialog();
                return true;
            } else if (id == R.id.menu_expires_before) {
                showDateFilterDialog("expiresBefore:");
                return true;
            } else if (id == R.id.menu_expires_after) {
                showDateFilterDialog("expiresAfter:");
                return true;
            } else if (id == R.id.menu_expires_on) {
                showDateFilterDialog("expires:");
                return true;
            } else if (id == R.id.menu_added_before) {
                showDateFilterDialog("addedBefore:");
                return true;
            } else if (id == R.id.menu_added_after) {
                showDateFilterDialog("addedAfter:");
                return true;
            } else if (id == R.id.menu_added_on) {
                showDateFilterDialog("added:");
                return true;
            } else if (id == R.id.menu_clear_filters) {
                clearFilters();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showByUserDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_input, null);
        TextInputEditText etValue = dialogView.findViewById(R.id.etFilterValue);

        new AlertDialog.Builder(this)
                .setTitle("Filter by User")
                .setView(dialogView)
                .setPositiveButton("Apply", (dialog, which) -> {
                    String value = etValue.getText() != null ? etValue.getText().toString().trim() : "";
                    if (!value.isEmpty()) {
                        appendFilter("by:" + value);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDateFilterDialog(String prefix) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String dateStr = sdf.format(selected.getTime());
                    appendFilter(prefix + dateStr);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    private void appendFilter(String filter) {
        String current = searchViewModel.getSearchQuery().getValue();
        if (current == null) current = "";
        current = current.trim();
        if (current.isEmpty()) {
            searchView.setQuery(filter, false);
        } else {
            searchView.setQuery(current + ", " + filter, false);
        }
        searchViewModel.setSearchQuery(searchView.getQuery().toString());
        updateFilterBadge();
    }

    private void clearFilters() {
        searchView.setQuery("", false);
        searchViewModel.setSearchQuery("");
        updateFilterBadge();
    }

    private void updateFilterBadge() {
        String query = searchViewModel.getSearchQuery().getValue();
        if (query == null || query.trim().isEmpty()) {
            filterBadge.setVisibility(View.GONE);
        } else {
            String[] parts = query.split(",");
            int filterCount = 0;
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.contains(":") && !trimmed.startsWith("by:") ||
                        (trimmed.startsWith("by:") && trimmed.length() > 3)) {
                    filterCount++;
                }
            }
            if (filterCount > 0) {
                filterBadge.setText(String.valueOf(filterCount));
                filterBadge.setVisibility(View.VISIBLE);
            } else {
                filterBadge.setVisibility(View.GONE);
            }
        }
    }

    public void addItem(String listType, PlaceholderContent.PlaceholderItem item, int position) {
        String userName = getCurrentUserName();
        apiClient.addItem(listType, item, userName, new ListRequestManager.ListCallback() {
            @Override
            public void onSuccess(String commandId, String itemId) {
                item.id = itemId;
                runOnUiThread(() -> {
                    List<PlaceholderContent.PlaceholderItem> targetList =
                            listType.equals("whitelist") ? PlaceholderContent.ITEMS : PlaceholderContent.BLACKLIST_ITEMS;
                    targetList.add(item);
                    refreshFragment(listType);
                    Toast.makeText(listsActivity.this, "Item added", Toast.LENGTH_SHORT).show();
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getEmail() : "unknown";
    }
}
