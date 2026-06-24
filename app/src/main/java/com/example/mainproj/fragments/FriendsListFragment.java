package com.example.mainproj.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.example.mainproj.messaging.MessagingActivity;
import com.example.mainproj.messaging.friendRequest.Friend;
import com.example.mainproj.messaging.friendRequest.FriendsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FriendsListFragment extends DialogFragment {

    private RecyclerView recyclerView;
    private FriendsAdapter adapter;
    private List<Friend> friends = new ArrayList<>();
    private DatabaseReference friendsRef;
    private ValueEventListener friendsListener;
    private String currentUserSafeEmail;
    private View loadingState;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_friends_list, null);
        builder.setView(view);
        builder.setNegativeButton("Close", null);

        recyclerView = view.findViewById(R.id.recyclerView);
        loadingState = view.findViewById(R.id.loadingState);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FriendsAdapter(requireContext(), new FriendsAdapter.OnFriendClickListener() {
            @Override
            public void onFriendClick(Friend friend) {
                openChat(friend);
            }

            @Override
            public void onFriendLongClick(Friend friend) {
                showRemoveFriendDialog(friend);
            }
        });
        recyclerView.setAdapter(adapter);

        loadFriends();

        return builder.create();
    }

    private void loadFriends() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUserSafeEmail = user.getEmail().replace(".", "_");
        friendsRef = FirebaseDatabase.getInstance()
                .getReference("users").child(currentUserSafeEmail).child("friends");

        friendsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (loadingState != null) loadingState.setVisibility(View.GONE);
                friends.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Friend friend = snap.getValue(Friend.class);
                    if (friend != null) {
                        friend.setUsername(snap.getKey());
                        friends.add(friend);
                    }
                }
                adapter.setFriends(friends);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (loadingState != null) loadingState.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error loading friends", Toast.LENGTH_SHORT).show();
            }
        };
        friendsRef.addValueEventListener(friendsListener);
    }

    private void openChat(Friend friend) {
        Intent intent = new Intent(requireContext(), MessagingActivity.class);
        intent.putExtra("chatId", friend.getChatId());
        intent.putExtra("username", friend.getUsername());
        startActivity(intent);
    }

    private void showRemoveFriendDialog(Friend friend) {
        String displayName = friend.getNickname() != null && !friend.getNickname().isEmpty()
                ? friend.getNickname() : friend.getUsername();

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Friend")
                .setMessage("Are you sure you want to remove " + displayName + " from your friends?")
                .setPositiveButton("Remove", (dialog, which) -> removeFriend(friend))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeFriend(Friend friend) {
        if (currentUserSafeEmail == null || friend.getSafeEmail() == null) return;

        FirebaseDatabase.getInstance().getReference("users")
                .child(currentUserSafeEmail).child("friends").child(friend.getUsername())
                .removeValue();

        FirebaseDatabase.getInstance().getReference("users").child(currentUserSafeEmail).child("username")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String myUsername = snapshot.getValue(String.class);
                        if (myUsername != null) {
                            FirebaseDatabase.getInstance().getReference("users")
                                    .child(friend.getSafeEmail()).child("friends").child(myUsername)
                                    .removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        Toast.makeText(requireContext(), "Friend removed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (friendsRef != null && friendsListener != null) {
            friendsRef.removeEventListener(friendsListener);
        }
    }
}
