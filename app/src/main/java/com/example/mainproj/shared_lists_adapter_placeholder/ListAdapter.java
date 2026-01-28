package com.example.mainproj.shared_lists_adapter_placeholder;

import android.view.*;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.VH> {

    private final List<PlaceholderContent.PlaceholderItem> fullList;
    private final List<PlaceholderContent.PlaceholderItem> visibleList = new ArrayList<>();

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yy", Locale.getDefault());

    public ListAdapter(List<PlaceholderContent.PlaceholderItem> items) {
        this.fullList = new ArrayList<>(items);
        visibleList.addAll(items);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        PlaceholderContent.PlaceholderItem item = visibleList.get(position);

        holder.name.setText(item.name);
        holder.byWho.setText("By: " + item.byWho);
        holder.reason.setText("Reason: " + item.reason);
        holder.timestampAdded.setText("Added: " + formatTimestamp(item.timestampWhenAdded));

        holder.expiresAt.setText(
                item.timestampExpiresAt == -1
                        ? "Expires: Never"
                        : "Expires: " + formatTimestamp(item.timestampExpiresAt)
        );

        holder.itemView.setAlpha(item.isRemoved ? 0.5f : 1f);
    }

    @Override
    public int getItemCount() {
        return visibleList.size();
    }

    // 🔍 ENTRY POINT
    public void filter(String input) {
        visibleList.clear();

        if (input == null || input.trim().isEmpty()) {
            visibleList.addAll(fullList);
            notifyDataSetChanged();
            return;
        }

        FilterQuery q = FilterQuery.parse(input);

        for (PlaceholderContent.PlaceholderItem item : fullList) {
            if (matches(item, q)) {
                visibleList.add(item);
            }
        }

        notifyDataSetChanged();
    }

    // ✅ MATCH LOGIC
    private boolean matches(PlaceholderContent.PlaceholderItem item, FilterQuery q) {

        // text search (name)
        if (!q.text.isEmpty()
                && !item.name.toLowerCase().contains(q.text)) {
            return false;
        }

        // by:
        if (!q.by.isEmpty()
                && !q.by.contains(item.byWho.toLowerCase())) {
            return false;
        }

        // expires exact
        if (q.expires != null) {
            if (item.timestampExpiresAt == -1
                    || !sameDay(item.timestampExpiresAt, q.expires)) {
                return false;
            }
        }

        // expires before
        if (q.expiresBefore != null) {
            if (item.timestampExpiresAt == -1
                    || item.timestampExpiresAt >= q.expiresBefore) {
                return false;
            }
        }

        // expires after
        if (q.expiresAfter != null) {
            if (item.timestampExpiresAt == -1
                    || item.timestampExpiresAt <= q.expiresAfter) {
                return false;
            }
        }

        // added exact
        if (q.added != null) {
            if (!sameDay(item.timestampWhenAdded, q.added)) {
                return false;
            }
        }

        // added before
        if (q.addedBefore != null) {
            if (item.timestampWhenAdded >= q.addedBefore) {
                return false;
            }
        }

        // added after
        if (q.addedAfter != null) {
            if (item.timestampWhenAdded <= q.addedAfter) {
                return false;
            }
        }

        return true;
    }

    // 🧠 QUERY PARSER
    private static class FilterQuery {
        String text = "";
        Set<String> by = new HashSet<>();

        Long expires = null;
        Long expiresBefore = null;
        Long expiresAfter = null;

        Long added = null;
        Long addedBefore = null;
        Long addedAfter = null;

        static FilterQuery parse(String input) {
            FilterQuery q = new FilterQuery();

            for (String token : input.split("\\s+")) {
                token = token.trim();

                if (token.startsWith("by:")) {
                    for (String s : token.substring(3).split(",")) {
                        q.by.add(s.toLowerCase());
                    }
                }
                else if (token.startsWith("expiresBefore:")) {
                    q.expiresBefore = parseDate(token.substring(14));
                }
                else if (token.startsWith("expiresAfter:")) {
                    q.expiresAfter = parseDate(token.substring(13));
                }
                else if (token.startsWith("expires:")) {
                    q.expires = parseDate(token.substring(8));
                }
                else if (token.startsWith("addedBefore:")) {
                    q.addedBefore = parseDate(token.substring(12));
                }
                else if (token.startsWith("addedAfter:")) {
                    q.addedAfter = parseDate(token.substring(11));
                }
                else if (token.startsWith("added:")) {
                    q.added = parseDate(token.substring(6));
                }
                else {
                    q.text += token.toLowerCase() + " ";
                }
            }

            q.text = q.text.trim();
            return q;
        }

        static Long parseDate(String s) {
            try {
                return DATE_FORMAT.parse(s).getTime();
            } catch (ParseException e) {
                return null;
            }
        }
    }

    // 🗓 DAY COMPARISON
    private boolean sameDay(long a, long b) {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTimeInMillis(a);
        c2.setTimeInMillis(b);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm", Locale.getDefault()
        ).format(new Date(timestamp));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, byWho, reason, timestampAdded, expiresAt;

        VH(View v) {
            super(v);
            name = v.findViewById(R.id.item_name);
            byWho = v.findViewById(R.id.item_by_who);
            reason = v.findViewById(R.id.item_reason);
            timestampAdded = v.findViewById(R.id.item_added);
            expiresAt = v.findViewById(R.id.item_expires);
        }
    }
}
