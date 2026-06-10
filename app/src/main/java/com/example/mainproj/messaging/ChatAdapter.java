package com.example.mainproj.messaging;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mainproj.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ChatModel> chatList;
    private List<ChatModel> fullList;
    private Context context;
    private String safeEmail;

    public ChatAdapter(Context context, List<ChatModel> chatList, String safeEmail) {
        this.context = context;
        this.chatList = chatList;
        this.fullList = new ArrayList<>(chatList);
        this.safeEmail = safeEmail;
    }

    public void updateFullList(List<ChatModel> newList) {
        fullList = new ArrayList<>(newList);
    }

    public void filter(String text) {
        chatList.clear();
        if (TextUtils.isEmpty(text)) {
            chatList.addAll(fullList);
        } else {
            String search = text.toLowerCase();
            for (ChatModel item : fullList) {
                String name = item.getNickname() != null ? item.getNickname().toLowerCase() : "";
                String username = item.getUsername() != null ? item.getUsername().toLowerCase() : "";
                if (name.contains(search) || username.contains(search)) {
                    chatList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatModel chat = chatList.get(position);

        String displayName = chat.getNickname() != null && !chat.getNickname().isEmpty()
                ? chat.getNickname() : chat.getUsername();
        holder.tvName.setText(displayName);

        String chatId = chat.getChatId();
        boolean isGemini = chatId != null && chatId.endsWith("_gemini_ai");

        if (isGemini) {
            holder.imgProfile.setImageResource(R.drawable.ic_gemini);
            holder.onlineIndicator.setVisibility(View.VISIBLE);
            holder.unreadBadge.setVisibility(View.GONE);
            holder.tvLastMessage.setText(chat.getLastMessage() != null ? chat.getLastMessage() : "");
            holder.tvTime.setText("");

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, com.example.mainproj.messaging.MessagingActivity.class);
                intent.putExtra("chatId", chat.getChatId());
                intent.putExtra("username", chat.getUsername());
                context.startActivity(intent);
            });
            holder.itemView.setOnLongClickListener(null);
            return;
        }

        if (chat.getImageUrl() != null && !chat.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(chat.getImageUrl())
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(holder.imgProfile);
        } else {
            holder.imgProfile.setImageResource(R.drawable.ic_default_avatar);
        }

        if (chat.isOnline()) {
            holder.onlineIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.onlineIndicator.setVisibility(View.GONE);
        }

        Long unread = chat.getUnreadCount();
        if (unread != null && unread > 0) {
            holder.unreadBadge.setVisibility(View.VISIBLE);
            holder.unreadBadge.setText(String.valueOf(unread));
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
        }

        if (chatId != null) {
            if (!NetworkMonitor.getInstance(context).isConnected()) {
                holder.tvLastMessage.setText(chat.getLastMessage() != null ? chat.getLastMessage() : "");
                Long ts = chat.getTimestamp();
                holder.tvTime.setText(ts != null ? formatTimestamp(ts) : "");
            } else {
                DatabaseReference chatRef = FirebaseDatabase.getInstance()
                        .getReference("chats").child(chatId);
                chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String lastMessageText = "";
                        long lastTimestamp = 0;

                        if (snapshot.child("lastMessage").exists()) {
                            DataSnapshot lastMsgSnap = snapshot.child("lastMessage");
                            lastMessageText = lastMsgSnap.child("text").getValue(String.class);
                            String sender = lastMsgSnap.child("senderUsername").getValue(String.class);
                            String currentUsername = ChatCacheApplication.getCurrentUsername();
                            boolean isMe = sender != null && sender.equals(currentUsername);
                            if (lastMessageText != null && isMe) {
                                lastMessageText = "You" + ": " + lastMessageText;
                            }
                            Object tsObj = lastMsgSnap.child("timestamp").getValue();
                            if (tsObj instanceof Long) lastTimestamp = (Long) tsObj;
                            else if (tsObj instanceof Double) lastTimestamp = ((Double) tsObj).longValue();
                        }

                        holder.tvLastMessage.setText(lastMessageText != null ? lastMessageText : "");
                        holder.tvTime.setText(formatTimestamp(lastTimestamp));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.tvLastMessage.setText(chat.getLastMessage() != null ? chat.getLastMessage() : "");
                        Long ts = chat.getTimestamp();
                        holder.tvTime.setText(ts != null ? formatTimestamp(ts) : "");
                        Toast.makeText(context, "Failed to get from firebase", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, com.example.mainproj.messaging.MessagingActivity.class);
            intent.putExtra("chatId", chat.getChatId());
            intent.putExtra("username", chat.getUsername());
            context.startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return true;

            new AlertDialog.Builder(context)
                    .setTitle("Chat Options")
                    .setItems(new CharSequence[]{"Edit Name", "Delete Chat"}, (dialog, which) -> {
                        ChatModel selectedChat = chatList.get(adapterPos);

                        if (which == 0) showEditNameDialog(selectedChat, adapterPos);
                        else if (which == 1) {
                            chatList.remove(adapterPos);
                            fullList.removeIf(item -> item.getChatId() != null
                                    && item.getChatId().equals(selectedChat.getChatId()));
                            notifyDataSetChanged();

                            if (!NetworkMonitor.getInstance(context).isConnected()) {
                                if (context.getSharedPreferences("offline_snooze_prefs", Context.MODE_PRIVATE)
                                        .getLong("snoozed_until", 0) > System.currentTimeMillis()) {
                                    ChatMetadataManager.getInstance(context).clearChat(safeEmail, selectedChat.getChatId());
                                    ChatCacheManager.getInstance(context).clearChat(selectedChat.getChatId());
                                    Toast.makeText(context, "Chat removed", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                new AlertDialog.Builder(context)
                                        .setTitle("No Internet Connection")
                                        .setMessage("No internet connection, action will not be synced out of device.")
                                        .setPositiveButton("OK", (d, w) -> {
                                            ChatMetadataManager.getInstance(context).clearChat(safeEmail, selectedChat.getChatId());
                                            ChatCacheManager.getInstance(context).clearChat(selectedChat.getChatId());
                                            Toast.makeText(context, "Chat removed", Toast.LENGTH_SHORT).show();
                                        })
                                        .setNegativeButton("Don't show for 30 min", (d, w) -> {
                                            context.getSharedPreferences("offline_snooze_prefs", Context.MODE_PRIVATE)
                                                    .edit().putLong("snoozed_until", System.currentTimeMillis() + 1800000).apply();
                                            ChatMetadataManager.getInstance(context).clearChat(safeEmail, selectedChat.getChatId());
                                            ChatCacheManager.getInstance(context).clearChat(selectedChat.getChatId());
                                            Toast.makeText(context, "Chat removed", Toast.LENGTH_SHORT).show();
                                        })
                                        .setNeutralButton("Cancel", null)
                                        .show();
                                return;
                            }

                            Toast.makeText(context, "Chat removed", Toast.LENGTH_SHORT).show();
                            if (selectedChat.getChatId() != null) {
                                DatabaseReference userChatRef = FirebaseDatabase.getInstance()
                                        .getReference("users")
                                        .child(safeEmail)
                                        .child("chats")
                                        .child(selectedChat.getChatId());
                                userChatRef.removeValue();
                            }
                        }
                    }).show();
            return true;
        });
    }

    private void showEditNameDialog(ChatModel chat, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Chat Name");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        String currentName = chat.getNickname() != null && !chat.getNickname().isEmpty()
                ? chat.getNickname() : chat.getUsername();
        input.setText(currentName);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                chat.setNickname(newName);
                notifyItemChanged(position);

                for (ChatModel item : fullList) {
                    if (item.getChatId() != null && item.getChatId().equals(chat.getChatId())) {
                        item.setNickname(newName);
                        break;
                    }
                }

                if (chat.getChatId() != null) {
                    if (!NetworkMonitor.getInstance(context).isConnected()) {
                        if (context.getSharedPreferences("offline_snooze_prefs", Context.MODE_PRIVATE)
                                .getLong("snoozed_until", 0) > System.currentTimeMillis()) {
                            ChatMetadataManager.getInstance(context).saveChat(safeEmail, chat);
                            return;
                        }
                        new AlertDialog.Builder(context)
                                .setTitle("No Internet Connection")
                                .setMessage("No internet connection, action will not be synced out of device.")
                                .setPositiveButton("OK", (d, w) -> {
                                    ChatMetadataManager.getInstance(context).saveChat(safeEmail, chat);
                                })
                                .setNegativeButton("Don't show for 30 min", (d, w) -> {
                                    context.getSharedPreferences("offline_snooze_prefs", Context.MODE_PRIVATE)
                                            .edit().putLong("snoozed_until", System.currentTimeMillis() + 1800000).apply();
                                    ChatMetadataManager.getInstance(context).saveChat(safeEmail, chat);
                                })
                                .setNeutralButton("Cancel", null)
                                .show();
                        return;
                    }
                    DatabaseReference chatNameRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(safeEmail)
                            .child("chats")
                            .child(chat.getChatId())
                            .child("chatName");
                    chatNameRef.setValue(newName)
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Failed to update name", Toast.LENGTH_SHORT).show());
                }
            } else {
                input.setError("Name cannot be empty");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp == 0) return "";

        Calendar cal = Calendar.getInstance();
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTimeInMillis(timestamp);

        int today = cal.get(Calendar.DAY_OF_YEAR);
        int msgDay = msgCal.get(Calendar.DAY_OF_YEAR);
        int year = cal.get(Calendar.YEAR);
        int msgYear = msgCal.get(Calendar.YEAR);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        if (year == msgYear && today == msgDay) return timeFormat.format(msgCal.getTime());
        else if (year == msgYear && today - msgDay == 1) return "Yesterday";
        else return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(msgCal.getTime());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile;
        TextView tvName, tvLastMessage, tvTime;
        View onlineIndicator;
        TextView unreadBadge;

        ViewHolder(View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.imgProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
            unreadBadge = itemView.findViewById(R.id.unreadBadge);
        }
    }
}