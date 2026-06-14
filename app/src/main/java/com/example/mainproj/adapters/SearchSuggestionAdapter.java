package com.example.mainproj.adapters;

import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.SearchModule.suggestionsModule.SearchSuggestion;

import java.util.ArrayList;
import java.util.List;

public class SearchSuggestionAdapter
        extends RecyclerView.Adapter<SearchSuggestionAdapter.VH> {

    public interface OnSuggestionClick {
        void onClick(SearchSuggestion s);
    }

    private final List<SearchSuggestion> items = new ArrayList<>();
    private final OnSuggestionClick callback;

    public SearchSuggestionAdapter(OnSuggestionClick cb) {
        this.callback = cb;
    }

    // Update suggestions dynamically
    public void setItems(List<SearchSuggestion> newItems) {
        items.clear();
        if (newItems != null && !newItems.isEmpty()) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    // Clear all suggestions
    public void clearSuggestions() {
        items.clear();
        notifyDataSetChanged();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create container LinearLayout
        LinearLayout container = new LinearLayout(parent.getContext());
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(32, 24, 32, 24);
        container.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        container.setGravity(Gravity.CENTER_VERTICAL);

        // Create TextView inside container
        TextView tv = new TextView(parent.getContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        tv.setTextSize(16);

        container.addView(tv);
        return new VH(container, tv);
    }

    @Override
    public void onBindViewHolder(VH h, int position) {
        SearchSuggestion s = items.get(position);
        h.text.setText(s.display);

        // Click listener on the entire row
        h.container.setOnClickListener(v -> {
            callback.onClick(s);     // Trigger suggestion action
            clearSuggestions();      // Remove all suggestions from view
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout container;
        TextView text;

        VH(LinearLayout container, TextView text) {
            super(container);
            this.container = container;
            this.text = text;
        }
    }
}
