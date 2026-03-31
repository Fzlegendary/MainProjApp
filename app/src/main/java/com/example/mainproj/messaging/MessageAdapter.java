package com.example.mainproj.messaging;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;

import java.util.ArrayList;

/**
 * MessageAdapter
 *
 * Three view types:
 *   VIEW_TYPE_SENT          → item_message_sent.xml     (blue bubble, right-aligned, no avatar)
 *   VIEW_TYPE_RECEIVED     → item_message_received.xml (white bubble, left-aligned, avatar on left)
 *   VIEW_TYPE_DATE_SEPARATOR → item_date_separator.xml   (centered date pill)
 *
 * Tapping the avatar in a received message opens UserProfileBottomSheet.
 */
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT          = 1;
    private static final int VIEW_TYPE_RECEIVED      = 2;
    private static final int VIEW_TYPE_DATE_SEPARATOR = 3;

    private final ArrayList<MessageModel> messages;
    private final Context                 context;

    public MessageAdapter(ArrayList<MessageModel> messages, Context context) {
        this.messages = messages;
        this.context  = context;
    }

    // ── View type selection ───────────────────────────────────────────────────

    @Override
    public int getItemViewType(int position) {
        MessageModel msg = messages.get(position);
        if (msg.isDateSeparator()) {
            return VIEW_TYPE_DATE_SEPARATOR;
        }
        return msg.isSentByMe() ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    // ── Inflation ─────────────────────────────────────────────────────────────

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_SENT) {
            View v = inf.inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(v);
        } else if (viewType == VIEW_TYPE_RECEIVED) {
            View v = inf.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(v);
        } else {
            View v = inf.inflate(R.layout.item_date_separator, parent, false);
            return new DateSeparatorViewHolder(v);
        }
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel msg = messages.get(position);

        if (holder instanceof DateSeparatorViewHolder) {
            DateSeparatorViewHolder vh = (DateSeparatorViewHolder) holder;
            vh.dateLabel.setText(msg.getDateLabel());
        } else if (holder instanceof SentViewHolder) {
            SentViewHolder vh = (SentViewHolder) holder;
            vh.text.setText(msg.getText());
            vh.time.setText(msg.getTimestamp());
        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder vh = (ReceivedViewHolder) holder;
            vh.text.setText(msg.getText());
            vh.time.setText(msg.getTimestamp());

            // Tap the profile picture → open profile bottom sheet
            vh.avatar.setOnClickListener(v -> openProfileSheet(msg.getSenderUsername()));
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    // ── Profile sheet helper ──────────────────────────────────────────────────

    private void openProfileSheet(String username) {
        if (!(context instanceof FragmentActivity) || username == null) return;

        UserProfileBottomSheet sheet = UserProfileBottomSheet.newInstance(
                username, "", "", false
        );

        sheet.setActionListener(new UserProfileBottomSheet.ProfileActionListener() {
            @Override public void onSendMessage(String u) { /* already in this chat */ }
            @Override public void onViewProfile(String u) { /* TODO: open full profile */ }
            @Override public void onBlockUser(String u)   { /* TODO: block logic     */ }
        });

        sheet.show(
                ((FragmentActivity) context).getSupportFragmentManager(),
                UserProfileBottomSheet.TAG
        );
    }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView text, time;

        SentViewHolder(View v) {
            super(v);
            text = v.findViewById(R.id.msgSentText);
            time = v.findViewById(R.id.msgSentTime);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView  text, time;

        ReceivedViewHolder(View v) {
            super(v);
            avatar = v.findViewById(R.id.msgReceivedAvatar);
            text   = v.findViewById(R.id.msgReceivedText);
            time   = v.findViewById(R.id.msgReceivedTime);
        }
    }

    static class DateSeparatorViewHolder extends RecyclerView.ViewHolder {
        TextView dateLabel;

        DateSeparatorViewHolder(View v) {
            super(v);
            dateLabel = v.findViewById(R.id.tvDateSeparator);
        }
    }
}