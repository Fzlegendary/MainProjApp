package com.example.mainproj.shared_lists_adapter_placeholder;

import android.view.*;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mainproj.R;

import java.text.SimpleDateFormat;
import java.util.*;

import androidx.annotation.NonNull;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.VH> {

    public interface OnDeleteClickListener {
        void onDelete(int position, PlaceholderContent.PlaceholderItem item);
    }

    private final List<PlaceholderContent.PlaceholderItem> fullList;
    private final List<PlaceholderContent.PlaceholderItem> visibleList = new ArrayList<>();
    private OnDeleteClickListener deleteListener;

    public ListAdapter(List<PlaceholderContent.PlaceholderItem> items) {
        this.fullList = new ArrayList<>(items);
        visibleList.addAll(items);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
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

        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(holder.getAdapterPosition(), item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return visibleList.size();
    }

    public void update(List<PlaceholderContent.PlaceholderItem> filtered) {
        visibleList.clear();
        visibleList.addAll(filtered);
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position < 0 || position >= visibleList.size()) return;
        PlaceholderContent.PlaceholderItem item = visibleList.get(position);
        visibleList.remove(position);
        fullList.remove(item);
        notifyItemRemoved(position);
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, byWho, reason, timestampAdded, expiresAt;
        ImageButton deleteButton;

        VH(View v) {
            super(v);
            name = v.findViewById(R.id.item_name);
            byWho = v.findViewById(R.id.item_by_who);
            reason = v.findViewById(R.id.item_reason);
            timestampAdded = v.findViewById(R.id.item_added);
            expiresAt = v.findViewById(R.id.item_expires);
            deleteButton = v.findViewById(R.id.btnDelete);
        }
    }
}
