package com.example.mainproj.messaging.friendRequest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.mainproj.R;
import com.example.mainproj.messaging.UserProfileBottomSheet;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    private List<FriendRequest> requests = new ArrayList<>();
    private OnRequestActionListener listener;
    private final Context context;

    public interface OnRequestActionListener {
        void onAccept(FriendRequest request);
        void onReject(FriendRequest request);
    }

    public FriendRequestAdapter(Context context, OnRequestActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setRequests(List<FriendRequest> newRequests) {
        this.requests = newRequests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequest request = requests.get(position);
        holder.tvUsername.setText(request.getFromUsername());
        
        if (request.getFromImageUrl() != null && !request.getFromImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(request.getFromImageUrl())
                    .apply(com.bumptech.glide.request.RequestOptions.circleCropTransform()
                            .placeholder(R.drawable.bg_avatar_circle)
                            .error(R.drawable.bg_avatar_circle))
                    .into(holder.ivProfilePicture);
        } else {
            holder.ivProfilePicture.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        
        holder.ivProfilePicture.setOnClickListener(v -> {
            if (context instanceof FragmentActivity) {
                UserProfileBottomSheet sheet = UserProfileBottomSheet.newInstance(
                        request.getFromUsername(), "", "", false
                );
                sheet.show(
                        ((FragmentActivity) context).getSupportFragmentManager(),
                        UserProfileBottomSheet.TAG
                );
            }
        });
        
        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(request);
        });
        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(request);
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfilePicture;
        TextView tvUsername;
        Button btnAccept, btnReject;

        ViewHolder(View itemView) {
            super(itemView);
            ivProfilePicture = itemView.findViewById(R.id.ivProfilePicture);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}