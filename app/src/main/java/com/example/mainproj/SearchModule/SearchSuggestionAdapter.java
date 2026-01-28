package com.example.mainproj.SearchModule;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

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

    public void setItems(List<SearchSuggestion> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        // Wrap TextView inside a LinearLayout
        LinearLayout container = new LinearLayout(parent.getContext());
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(32, 24, 32, 24);
        container.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        container.setGravity(Gravity.CENTER_VERTICAL);

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
    public void onBindViewHolder(VH h, int p) {
        SearchSuggestion s = items.get(p);
        h.text.setText(s.display);

        // Set click listener on the entire row (LinearLayout)
        h.container.setOnClickListener(v -> callback.onClick(s));
    }

    @Override public int getItemCount() { return items.size(); }

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
