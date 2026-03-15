package com.example.mainproj.messaging;

// Android imports for context, intents, views, and layouts
import android.content.Context;        // used to access app resources and start activities
import android.content.Intent;         // used to navigate between activities
import android.view.LayoutInflater;    // inflates (converts) XML layout files into View objects
import android.view.View;              // base class for all UI components
import android.view.ViewGroup;         // a view that can contain other views (the list container)
import android.widget.TextView;        // displays text in the UI

// AndroidX imports for fragment support and RecyclerView
import androidx.fragment.app.FragmentActivity; // needed to get a FragmentManager for the bottom sheet
import androidx.recyclerview.widget.RecyclerView; // scrollable list component

// App-specific imports
import com.example.mainproj.R; // gives access to resources like layouts, ids, colors

// Java utility
import java.util.ArrayList; // dynamic list to hold chat items

// ChatAdapter connects the list of ChatModel data to the RecyclerView UI
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private ArrayList<ChatModel> chatList; // holds all chat items to display
    private final Context context;               // used to inflate views and start activities

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

        ChatModel chat = chatList.get(position); // get the chat data for this row's position

        holder.chatName.setText(chat.getUsername());       // set the username text
        holder.lastMessage.setText(chat.getLastMessage()); // set the last message preview text
        holder.chatTime.setText(chat.getTime());           // set the timestamp text

        // Short tap — opens the chat conversation screen
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class); // create intent to open ChatActivity
            intent.putExtra("username", chat.getUsername());          // pass the username to the next screen
            context.startActivity(intent);                            // launch ChatActivity
        });

        // Long press — opens the user profile bottom sheet dialog
        holder.itemView.setOnLongClickListener(v -> {
            showProfileSheet(chat); // call helper method to build and show the bottom sheet
            return true;            // return true to consume the event (prevents short tap also firing)
        });
    }

    // Builds and shows the UserProfileBottomSheet for the given chat item
    private void showProfileSheet(ChatModel chat) {

        // Bottom sheets require a FragmentManager, which only FragmentActivity has
        // If context is not a FragmentActivity (e.g. an Application context), bail out safely
        if (!(context instanceof FragmentActivity)) return;

        // Create a new instance of the bottom sheet, passing in the user's data
        UserProfileBottomSheet sheet = UserProfileBottomSheet.newInstance(
                chat.getUsername(),  // display name shown at the top of the sheet
                chat.getBio(),       // bio/status text shown below the divider
                chat.getLastSeen(),  // shown when the user is offline e.g. "Last seen 5m ago"
                chat.isOnline()      // true = show green dot + "Online", false = show last seen text
        );

        // Set up callbacks for each button inside the bottom sheet
        sheet.setActionListener(new UserProfileBottomSheet.ProfileActionListener() {

            @Override
            public void onSendMessage(String username) {
                // "Send Message" button tapped — open the chat, same as a short tap
                Intent intent = new Intent(context, ChatActivity.class); // intent to open ChatActivity
                intent.putExtra("username", username);                    // pass the username
                context.startActivity(intent);                            // launch the chat screen
            }

            @Override
            public void onViewProfile(String username) {
                // "View Full Profile" button tapped — navigate to the user's full profile screen
                // TODO: uncomment and replace ProfileActivity with your actual activity class name
                // Intent intent = new Intent(context, ProfileActivity.class);
                // intent.putExtra("username", username);
                // context.startActivity(intent);
            }

            @Override
            public void onBlockUser(String username) {
                // "Block User" button tapped — add your block/report logic here
                // TODO: call your ViewModel, repository, or Firebase method to block the user
            }
        });

        // Show the bottom sheet using the FragmentManager from the host Activity
        sheet.show(
                ((FragmentActivity) context).getSupportFragmentManager(), // get FragmentManager from Activity
                UserProfileBottomSheet.TAG  // unique tag to identify this dialog fragment
        );
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

        TextView chatName;    // displays the chat username
        TextView lastMessage; // displays the last message preview
        TextView chatTime;    // displays the time of the last message

        // Constructor — finds and stores each view by its ID from the inflated layout
        public ChatViewHolder(View itemView) {
            super(itemView);                                          // pass itemView to parent ViewHolder
            chatName    = itemView.findViewById(R.id.chatName);    // find the username TextView
            lastMessage = itemView.findViewById(R.id.lastMessage); // find the last message TextView
            chatTime    = itemView.findViewById(R.id.chatTime);    // find the timestamp TextView
        }
    }
}