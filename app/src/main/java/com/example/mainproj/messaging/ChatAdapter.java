package com.example.mainproj.messaging;

// Android imports for context, intents, views, and layouts
import android.app.AlertDialog;
import android.content.Context;        // used to access app resources and start activities
import android.content.Intent;         // used to navigate between activities
import android.util.Log;
import android.view.LayoutInflater;    // inflates (converts) XML layout files into View objects
import android.view.View;              // base class for all UI components
import android.view.ViewGroup;         // a view that can contain other views (the list container)
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;        // displays text in the UI

// AndroidX imports for fragment support and RecyclerView
import androidx.fragment.app.FragmentActivity; // needed to get a FragmentManager for the bottom sheet
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView; // scrollable list component

// App-specific imports
import com.example.mainproj.MainActivity;
import com.example.mainproj.R; // gives access to resources like layouts, ids, colors
import com.google.firebase.Firebase;

// Java utility
import java.text.SimpleDateFormat;
import java.util.ArrayList; // dynamic list to hold chat items
import java.util.Date;
import java.util.Locale;

// ChatAdapter connects the list of ChatModel data to the RecyclerView UI
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private String TAG = "chatAdapter";
    private ArrayList<ChatModel> chatList; // holds all chat items to display
    private final Context context;               // used to inflate views and start activities

    public interface OnChatActionListener {
        void onOpenProfile(ChatModel chat);
        void onCloseDM(ChatModel chat);
        void onChatClick(ChatModel chat);
    }

    private OnChatActionListener actionListener;

    public void setOnChatActionListener(OnChatActionListener listener) {
        this.actionListener = listener;
    }

    // Constructor — called when creating the adapter, receives the data list and context
    public ChatAdapter(ArrayList<ChatModel> chatList, Context context) {
        this.chatList = chatList; // store the list of chats
        this.context = context;   // store the context (usually the Activity)
    }

    // Called by RecyclerView when it needs a new row view — inflates the XML layout for one chat item
    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.chat_item, parent, false); // turn chat_item.xml into an actual View
        return new ChatViewHolder(view); // wrap the view in a ViewHolder and return it
    }

    // Called for each visible row — binds data from ChatModel into the views for that row
    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {

        ChatModel chat = chatList.get(position);
        if (chat == null) return;

        String username = chat.getUsername();
        if (username == null) username = "";
        String nickname = chat.getNickname();
        if (nickname == null) nickname = "";
        String displayName = !nickname.isEmpty() ? nickname : username;
        String bio = chat.getBio();
        if (bio == null) bio = "";
        Long lastSeen = chat.getLastSeen();
        final String chatid = chat.getChatId();

        // Create final copies of the method parameters so they can be safely captured by the lambda expression.
        // In Java, variables captured by a lambda must be effectively final (not reassigned).
        final String finalUsername = username;
        final String finalBio = bio;
        final Long finalLastSeen = lastSeen;


        holder.chatName.setText(displayName);
        String lastMsg = chat.getLastMessage();
        holder.lastMessage.setText(lastMsg != null ? lastMsg : "");
        String time = chat.getTime();
        holder.chatTime.setText(time != null ? time : "");

        // Load the profile image using Glide with circular crop.
        // If imageUrl is null or empty, show the default app icon.
        String imageUrl = chat.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .apply(RequestOptions.circleCropTransform()
                            .placeholder(R.drawable.bg_avatar_circle)
                            .error(R.drawable.bg_avatar_circle))
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        // Show the online status indicator only when the user is online.
        // Hidden (gone) when the user is offline or lastSeen is available.
        holder.onlineDot.setVisibility(chat.isOnline() ? View.VISIBLE : View.GONE);

        // Show the unread message count badge when there are unread messages.
        // Hidden when unreadCount is 0.
        Long unreadCountLong = chat.getUnreadCount();
        int unreadCount = unreadCountLong != null ? unreadCountLong.intValue() : 0;
        if (unreadCount > 0) {
            holder.unreadBadge.setVisibility(View.VISIBLE);
            // Cap display at "99+" for very large counts.
            holder.unreadBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
        }

        // Short tap — opens the chat conversation screen
        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                boolean b = chat.getChatId()==null;
                actionListener.onChatClick(chat);
            }
        });

        // Long press — opens the options dialog
        holder.itemView.setOnLongClickListener(v -> {
            showChatOptionsDialog(finalUsername, displayName, finalBio, finalLastSeen, chat.isOnline(), imageUrl, unreadCount);
            return true;
        });
    }

private void showChatOptionsDialog(String username, String displayName, String bio, Long lastSeen, boolean online, String imageUrl, int unreadCount) {
        String[] options = {"Open Profile", "Close DM"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(displayName);

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                if (actionListener != null) {
                    actionListener.onOpenProfile(buildChatFromData(username, displayName, bio, lastSeen, online, imageUrl, unreadCount));
                } else {
                    showProfileSheet(username, bio, lastSeen, online);
                }
            } else if (which == 1) {
                if (actionListener != null) {
                    actionListener.onCloseDM(buildChatFromData(username, displayName, bio, lastSeen, online, imageUrl, unreadCount));
                }
            }
        });

        builder.show();
    }

    private void showProfileSheet(String username, String bio, Long lastSeen, boolean online) {
        if (!(context instanceof FragmentActivity)) return;

        String lastSeenStr = "";
        if (lastSeen != null && lastSeen != 0) {
            lastSeenStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(lastSeen));
        }

        UserProfileBottomSheet sheet = UserProfileBottomSheet.newInstance(
                username,
                bio,
                lastSeenStr,
                online
        );

        sheet.setActionListener(new UserProfileBottomSheet.ProfileActionListener() {
            @Override
            public void onSendMessage(String user) {
                ChatModel chat = buildChatFromData(user, user, "", null, false, "", 0);
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("tab_position", 1);
                context.startActivity(intent);
            }

            @Override
            public void onViewProfile(String user) {
            }

            @Override
            public void onBlockUser(String user) {
            }
        });

        sheet.show(
                ((FragmentActivity) context).getSupportFragmentManager(),
                UserProfileBottomSheet.TAG
        );
    }

    private ChatModel buildChatFromData(String username, String displayName, String bio, Long lastSeen, boolean online, String imageUrl, long unreadCount) {
        ChatModel chat = new ChatModel();
        chat.setUsername(username);
        chat.setNickname(displayName);
        chat.setBio(bio);
        chat.setLastSeen(lastSeen);
        chat.setOnline(online);
        chat.setImageUrl(imageUrl);
        chat.setUnreadCount(unreadCount);
        return chat;
    }

    // Returns the total number of rows in the list — RecyclerView uses this to know when to stop
    @Override
    public int getItemCount() {
        return chatList.size(); // size of the chat list = number of rows
    }

    // -------------------------------------------------------------------------
    // ViewHolder — holds references to the views inside one chat row
    // Avoids calling findViewById repeatedly on every scroll (improves performance)
    // -------------------------------------------------------------------------
    public static class ChatViewHolder extends RecyclerView.ViewHolder {

        ImageView profileImage;  // displays the user's circular profile picture
        TextView chatName;       // displays the chat username
        TextView lastMessage;    // displays the last message preview
        TextView chatTime;       // displays the time of the last message
        View onlineDot;          // shows the green online status indicator
        TextView unreadBadge;    // shows the red unread message count badge

        // Constructor — finds and stores each view by its ID from the inflated layout
        public ChatViewHolder(View itemView) {
            super(itemView);                                          // pass itemView to parent ViewHolder
            profileImage = itemView.findViewById(R.id.profileImage); // find the profile image ImageView
            chatName     = itemView.findViewById(R.id.chatName);     // find the username TextView
            lastMessage  = itemView.findViewById(R.id.lastMessage);  // find the last message TextView
            chatTime     = itemView.findViewById(R.id.chatTime);     // find the timestamp TextView
            onlineDot    = itemView.findViewById(R.id.onlineDot);   // find the online status dot View
            unreadBadge  = itemView.findViewById(R.id.unreadBadge); // find the unread badge TextView
        }
    }
}
