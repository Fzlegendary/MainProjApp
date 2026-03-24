package com.example.mainproj.messaging.friendRequest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    private List<FriendRequest> requests = new ArrayList<>();
    private OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onAccept(FriendRequest request);
        void onReject(FriendRequest request);
    }

    public FriendRequestAdapter(OnRequestActionListener listener) {
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
        TextView tvUsername;
        Button btnAccept, btnReject;

        ViewHolder(View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}