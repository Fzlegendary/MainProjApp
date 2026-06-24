package com.example.mainproj.shared_lists_adapter_placeholder;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.*;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mainproj.R;

import java.text.SimpleDateFormat;
import java.util.*;

import androidx.annotation.NonNull;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.VH> {

    public interface OnItemActionClickListener {
        void onDelete(int position, PlaceholderContent.PlaceholderItem item);
        void onRestore(int position, PlaceholderContent.PlaceholderItem item);
    }

    private final List<PlaceholderContent.PlaceholderItem> fullList;
    private final List<PlaceholderContent.PlaceholderItem> visibleList = new ArrayList<>();
    private OnItemActionClickListener actionListener;

    public ListAdapter(List<PlaceholderContent.PlaceholderItem> items) {
        this.fullList = new ArrayList<>(items);
        visibleList.addAll(items);
    }

    public void setOnItemActionClickListener(OnItemActionClickListener listener) {
        this.actionListener = listener;
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

        if (item.isExpired) {
            holder.name.setPaintFlags(holder.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.name.setAlpha(0.6f);
            holder.byWho.setAlpha(0.6f);
            holder.reason.setAlpha(0.6f);
            holder.timestampAdded.setAlpha(0.6f);

            long remaining = item.deleteAt - System.currentTimeMillis();
            holder.expiresAt.setText("Expired \u2014 deleting in " + formatTimeRemaining(Math.max(0, remaining)));
            holder.expiresAt.setTextColor(Color.parseColor("#BF616A"));

            holder.itemView.setAlpha(0.7f);

            holder.actionButton.setBackgroundResource(R.drawable.bg_fab_restore);
            holder.actionButton.setImageResource(R.drawable.ic_restore);
            holder.actionButton.setContentDescription("Restore item");
            holder.actionButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onRestore(holder.getAdapterPosition(), item);
                }
            });
        } else {
            holder.name.setPaintFlags(0);
            holder.name.setAlpha(1f);
            holder.byWho.setAlpha(1f);
            holder.reason.setAlpha(1f);
            holder.timestampAdded.setAlpha(1f);

            holder.expiresAt.setText(item.timestampExpiresAt == -1
                    ? "Expires: Never"
                    : "Expires: " + formatTimestamp(item.timestampExpiresAt));
            holder.expiresAt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorTextHint));

            holder.itemView.setAlpha(item.isRemoved ? 0.5f : 1f);

            holder.actionButton.setBackgroundResource(R.drawable.bg_fab_delete);
            holder.actionButton.setImageResource(R.drawable.ic_delete);
            holder.actionButton.setContentDescription("Delete item");
            holder.actionButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onDelete(holder.getAdapterPosition(), item);
                }
            });
        }
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

    private String formatTimeRemaining(long millis) {
        if (millis <= 0) return "any moment";
        long minutes = millis / 60000;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        minutes = minutes % 60;
        return hours + "h " + minutes + "m";
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, byWho, reason, timestampAdded, expiresAt;
        ImageButton actionButton;

        VH(View v) {
            super(v);
            name = v.findViewById(R.id.item_name);
            byWho = v.findViewById(R.id.item_by_who);
            reason = v.findViewById(R.id.item_reason);
            timestampAdded = v.findViewById(R.id.item_added);
            expiresAt = v.findViewById(R.id.item_expires);
            actionButton = v.findViewById(R.id.btnDelete);
        }
    }
}
