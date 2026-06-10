package com.example.mainproj.messaging;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mainproj.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_DATE_SEPARATOR = 0;
    private static final int VIEW_TYPE_SENT     = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_IMAGE_SENT = 3;
    private static final int VIEW_TYPE_IMAGE_RECEIVED = 4;

    private final ArrayList<MessageModel> messages;
    private final Context                 context;
    private OnMessageDeleteListener       deleteListener;

    public interface OnMessageDeleteListener {
        void onDeleteMessage(MessageModel message);
    }

    public MessageAdapter(ArrayList<MessageModel> messages, Context context) {
        this.messages = messages;
        this.context  = context;
        setHasStableIds(true);
    }

    public void setOnMessageDeleteListener(OnMessageDeleteListener listener) {
        this.deleteListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel msg = messages.get(position);
        if ("date_separator".equals(msg.getSenderUsername())) {
            return VIEW_TYPE_DATE_SEPARATOR;
        }
        if (msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {
            return msg.isSentByMe() ? VIEW_TYPE_IMAGE_SENT : VIEW_TYPE_IMAGE_RECEIVED;
        }
        return msg.isSentByMe() ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @Override
    public long getItemId(int position) {
        MessageModel msg = messages.get(position);
        return msg.getMessageId() != null ? msg.getMessageId().hashCode() : position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(context);

        if (viewType == VIEW_TYPE_DATE_SEPARATOR) {
            return new DateSeparatorViewHolder(
                    inf.inflate(R.layout.item_date_separator, parent, false)
            );
        } else if (viewType == VIEW_TYPE_SENT) {
            return new SentViewHolder(
                    inf.inflate(R.layout.item_message_sent, parent, false)
            );
        } else if (viewType == VIEW_TYPE_RECEIVED) {
            return new ReceivedViewHolder(
                    inf.inflate(R.layout.item_message_received, parent, false)
            );
        } else if (viewType == VIEW_TYPE_IMAGE_SENT) {
            return new ImageSentViewHolder(
                    inf.inflate(R.layout.item_message_image_sent, parent, false)
            );
        } else {
            return new ImageReceivedViewHolder(
                    inf.inflate(R.layout.item_message_image_received, parent, false)
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel msg = messages.get(position);

        if (holder instanceof DateSeparatorViewHolder) {
            DateSeparatorViewHolder vh = (DateSeparatorViewHolder) holder;
            vh.tvDate.setText(msg.getText());

        } else if (holder instanceof SentViewHolder) {
            SentViewHolder vh = (SentViewHolder) holder;
            vh.tvMessage.setText(msg.getText());
            vh.tvMessageTime.setText(formatTime(msg.getTimestamp()));

            vh.itemView.setOnLongClickListener(v -> {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION) return true;

                MessageModel msgToDelete = messages.get(adapterPos);
                new AlertDialog.Builder(context)
                        .setTitle("Delete Message")
                        .setMessage("Are you sure you want to delete this message?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            int pos = holder.getAdapterPosition();
                            if (pos == RecyclerView.NO_POSITION) return;
                            MessageModel m = messages.get(pos);
                            if (deleteListener != null) {
                                deleteListener.onDeleteMessage(m);
                            }
                            messages.remove(pos);
                            notifyItemRemoved(pos);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });

        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder vh = (ReceivedViewHolder) holder;
            vh.tvMessageNickname.setText(msg.getSenderUsername());
            vh.tvMessage.setText(msg.getText());
            vh.tvMessageTime.setText(formatTime(msg.getTimestamp()));

        } else if (holder instanceof ImageSentViewHolder) {
            ImageSentViewHolder vh = (ImageSentViewHolder) holder;
            loadImage(vh.ivMessageImage, msg.getImageUrl());
            vh.tvMessageTime.setText(formatTime(msg.getTimestamp()));
            if (msg.getText() != null && !msg.getText().isEmpty()) {
                vh.tvMessageCaption.setVisibility(View.VISIBLE);
                vh.tvMessageCaption.setText(msg.getText());
            } else {
                vh.tvMessageCaption.setVisibility(View.GONE);
            }

            vh.ivMessageImage.setOnClickListener(v -> showImageFullscreen(msg.getImageUrl()));

            vh.itemView.setOnLongClickListener(v -> {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION) return true;

                new AlertDialog.Builder(context)
                        .setTitle("Delete Message")
                        .setMessage("Are you sure you want to delete this message?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            int pos = holder.getAdapterPosition();
                            if (pos == RecyclerView.NO_POSITION) return;
                            MessageModel m = messages.get(pos);
                            if (deleteListener != null) {
                                deleteListener.onDeleteMessage(m);
                            }
                            messages.remove(pos);
                            notifyItemRemoved(pos);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });

        } else if (holder instanceof ImageReceivedViewHolder) {
            ImageReceivedViewHolder vh = (ImageReceivedViewHolder) holder;
            vh.tvMessageNickname.setText(msg.getSenderUsername());
            loadImage(vh.ivMessageImage, msg.getImageUrl());
            vh.tvMessageTime.setText(formatTime(msg.getTimestamp()));
            if (msg.getText() != null && !msg.getText().isEmpty()) {
                vh.tvMessageCaption.setVisibility(View.VISIBLE);
                vh.tvMessageCaption.setText(msg.getText());
            } else {
                vh.tvMessageCaption.setVisibility(View.GONE);
            }

            vh.ivMessageImage.setOnClickListener(v -> showImageFullscreen(msg.getImageUrl()));
        }
    }

    private void showImageFullscreen(String imageUrl) {
        Intent intent = new Intent(context, ImageViewerActivity.class);
        intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URL, imageUrl);
        context.startActivity(intent);
    }

    private String formatTime(long millis) {
        if (millis <= 0) return "";
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(millis));
    }

    private void loadImage(ImageView imageView, String url) {
        if (url == null || url.isEmpty()) return;

        if (url.startsWith("http")) {
            Glide.with(context)
                    .load(url)
                    .placeholder(com.google.android.material.R.drawable.mtrl_ic_error)
                    .fitCenter()
                    .into(imageView);
        } else {
            try {
                byte[] decodedBytes = Base64.decode(url, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                imageView.setImageResource(com.google.android.material.R.drawable.mtrl_ic_error);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class DateSeparatorViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;

        DateSeparatorViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvDate);
        }
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvMessageTime;

        SentViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvMessageTime = v.findViewById(R.id.tvMessageTime);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageNickname, tvMessage, tvMessageTime;

        ReceivedViewHolder(View v) {
            super(v);
            tvMessageNickname = v.findViewById(R.id.tvMessageNickname);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvMessageTime = v.findViewById(R.id.tvMessageTime);
        }
    }

    static class ImageSentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMessageImage;
        TextView tvMessageCaption, tvMessageTime;

        ImageSentViewHolder(View v) {
            super(v);
            ivMessageImage = v.findViewById(R.id.ivMessageImage);
            tvMessageCaption = v.findViewById(R.id.tvMessageCaption);
            tvMessageTime = v.findViewById(R.id.tvMessageTime);
        }
    }

    static class ImageReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageNickname;
        ImageView ivMessageImage;
        TextView tvMessageCaption, tvMessageTime;

        ImageReceivedViewHolder(View v) {
            super(v);
            tvMessageNickname = v.findViewById(R.id.tvMessageNickname);
            ivMessageImage = v.findViewById(R.id.ivMessageImage);
            tvMessageCaption = v.findViewById(R.id.tvMessageCaption);
            tvMessageTime = v.findViewById(R.id.tvMessageTime);
        }
    }
}