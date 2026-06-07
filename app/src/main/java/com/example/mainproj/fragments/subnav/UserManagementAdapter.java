package com.example.mainproj.fragments.subnav;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mainproj.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserManagementAdapter extends RecyclerView.Adapter<UserManagementAdapter.ViewHolder> {

    public static class UserListItem {
        public String safeEmail;
        public String username;
        public String email;
        public String phone;
        public String bio;
        public String profileImageUrl;
        public String role;
        public int roleLevel;
        public boolean online;
        public long lastSeen;
        public long createdAt;
    }

    public interface OnUserClickListener {
        void onUserClick(UserListItem user);
    }

    private final Context context;
    private List<UserListItem> items;
    private final OnUserClickListener listener;

    public UserManagementAdapter(Context context, List<UserListItem> items, OnUserClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<UserListItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserListItem user = items.get(position);
        holder.tvUsername.setText(user.username);
        holder.tvEmail.setText(user.email != null ? user.email : "");

        if (user.phone != null && !user.phone.isEmpty()) {
            holder.tvPhone.setVisibility(View.VISIBLE);
            holder.tvPhone.setText(user.phone);
        } else {
            holder.tvPhone.setVisibility(View.GONE);
        }

        if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
            Glide.with(context)
                    .load(user.profileImageUrl)
                    .apply(com.bumptech.glide.request.RequestOptions.circleCropTransform()
                            .placeholder(R.drawable.bg_avatar_circle)
                            .error(R.drawable.bg_avatar_circle))
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        if (user.online) {
            holder.tvStatus.setText("Online");
            holder.statusDot.setVisibility(View.VISIBLE);
        } else {
            holder.tvStatus.setText(formatLastSeen(user.lastSeen));
            holder.statusDot.setVisibility(View.INVISIBLE);
        }

        String roleText;
        int badgeColor;
        switch (user.roleLevel) {
            case 0:
                roleText = "User";
                badgeColor = ContextCompat.getColor(context, R.color.colorBadgeGray);
                break;
            case 1:
                roleText = "Admin";
                badgeColor = ContextCompat.getColor(context, R.color.colorPrimary);
                break;
            default:
                roleText = "Super Admin";
                badgeColor = ContextCompat.getColor(context, R.color.colorWarning);
                break;
        }
        holder.tvRoleBadge.setText(roleText);
        GradientDrawable badgeBg = (GradientDrawable) holder.tvRoleBadge.getBackground().mutate();
        badgeBg.setColor(badgeColor);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(user);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    private String formatLastSeen(long timestamp) {
        if (timestamp <= 0) return "Last seen recently";
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        if (diff < 60_000) return "Just now";
        if (diff < 3_600_000) return (diff / 60_000) + "m ago";
        if (diff < 86_400_000) return (diff / 3_600_000) + "h ago";
        if (diff < 604_800_000) return (diff / 86_400_000) + " days ago";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUsername, tvEmail, tvPhone, tvStatus, tvRoleBadge;
        View statusDot;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvRoleBadge = itemView.findViewById(R.id.tvRoleBadge);
            statusDot = itemView.findViewById(R.id.statusDot);
        }
    }
}
