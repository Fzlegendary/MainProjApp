package com.example.mainproj.messaging.friendRequest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mainproj.R;

import java.util.ArrayList;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private List<Friend> friends = new ArrayList<>();
    private final Context context;
    private final OnFriendClickListener listener;

    public interface OnFriendClickListener {
        void onFriendClick(Friend friend);
        void onFriendLongClick(Friend friend);
    }

    public FriendsAdapter(Context context, OnFriendClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setFriends(List<Friend> friends) {
        this.friends = friends;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = friends.get(position);
        String displayName = friend.getNickname() != null && !friend.getNickname().isEmpty()
                ? friend.getNickname() : friend.getUsername();
        holder.tvUsername.setText(displayName);

        if (friend.getImageUrl() != null && !friend.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(friend.getImageUrl())
                    .apply(com.bumptech.glide.request.RequestOptions.circleCropTransform()
                            .placeholder(R.drawable.bg_avatar_circle)
                            .error(R.drawable.bg_avatar_circle))
                    .into(holder.ivProfilePicture);
        } else {
            holder.ivProfilePicture.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFriendClick(friend);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onFriendLongClick(friend);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfilePicture;
        TextView tvUsername;

        ViewHolder(View itemView) {
            super(itemView);
            ivProfilePicture = itemView.findViewById(R.id.ivProfilePicture);
            tvUsername = itemView.findViewById(R.id.tvUsername);
        }
    }
}
