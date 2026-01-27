package com.example.mainproj.shared_lists_adapter_placeholder;

import android.view.*;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mainproj.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.VH> {

    private final List<PlaceholderContent.PlaceholderItem> items;

    public ListAdapter(List<PlaceholderContent.PlaceholderItem> items) {
        this.items = items;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        PlaceholderContent.PlaceholderItem item = items.get(position);

        // Display name
        holder.name.setText(item.name);

        // Display who added it
        holder.byWho.setText("By: " + item.byWho);

        // Display reason
        holder.reason.setText("Reason: " + item.reason);

        // Display timestampWhenAdded
        holder.timestampAdded.setText("Added: " + formatTimestamp(item.timestampWhenAdded));

        // Display expiration (or "Never")
        String expiresText = item.timestampExpiresAt == -1
                ? "Never"
                : formatTimestamp(item.timestampExpiresAt);
        holder.expiresAt.setText("Expires: " + expiresText);

        // If removed, gray out or strike-through
        if (item.isRemoved) {
            holder.itemView.setAlpha(0.5f);
        } else {
            holder.itemView.setAlpha(1f);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
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
