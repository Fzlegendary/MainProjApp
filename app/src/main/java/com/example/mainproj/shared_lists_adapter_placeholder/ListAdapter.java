package com.example.mainproj.shared_lists_adapter_placeholder;

import android.view.*;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mainproj.R;

import java.text.SimpleDateFormat;
import java.util.*;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.VH> {

    private final List<PlaceholderContent.PlaceholderItem> fullList;
    private final List<PlaceholderContent.PlaceholderItem> visibleList = new ArrayList<>();

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
        holder.expiresAt.setText(item.timestampExpiresAt == -1
                ? "Expires: Never"
                : "Expires: " + formatTimestamp(item.timestampExpiresAt));
        holder.itemView.setAlpha(item.isRemoved ? 0.5f : 1f);
    }

    @Override
    public int getItemCount() {
        return visibleList.size();
    }

    /** Update visible list after filtering */
    public void update(List<PlaceholderContent.PlaceholderItem> filtered) {
        visibleList.clear();
        visibleList.addAll(filtered);
        notifyDataSetChanged();
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(timestamp));
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
