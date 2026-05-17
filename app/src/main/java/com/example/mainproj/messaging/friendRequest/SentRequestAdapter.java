package com.example.mainproj.messaging.friendRequest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;

import java.util.List;

public class SentRequestAdapter extends RecyclerView.Adapter<SentRequestAdapter.SentRequestViewHolder> {

    private final Context context;
    private final List<SentRequest> requests;

    public SentRequestAdapter(Context context, List<SentRequest> requests) {
        this.context = context;
        this.requests = requests;
    }

    @NonNull
    @Override
    public SentRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_sent_request, parent, false);
        return new SentRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SentRequestViewHolder holder, int position) {
        SentRequest request = requests.get(position);
        holder.tvUsername.setText(request.getToUsername() != null ? request.getToUsername() : "Unknown");

        String status = request.getStatus();
        if (status == null) {
            status = "pending";
        }
        holder.tvStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));

        int statusColor;
        switch (status.toLowerCase()) {
            case "accepted":
                statusColor = android.R.color.holo_green_dark;
                break;
            case "declined":
                statusColor = android.R.color.holo_red_dark;
                break;
            default:
                statusColor = R.color.colorPrimary;
                break;
        }
        holder.tvStatus.setBackgroundTintList(context.getColorStateList(statusColor));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class SentRequestViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfilePicture;
        TextView tvUsername;
        TextView tvStatus;

        public SentRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePicture = itemView.findViewById(R.id.ivProfilePicture);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}