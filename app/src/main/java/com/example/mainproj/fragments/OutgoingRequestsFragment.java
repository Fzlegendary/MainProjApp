package com.example.mainproj.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.example.mainproj.messaging.friendRequest.SentRequest;
import com.example.mainproj.messaging.friendRequest.SentRequestAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class OutgoingRequestsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SentRequestAdapter adapter;
    private List<SentRequest> requests = new ArrayList<>();
    private DatabaseReference sentRef;
    private ValueEventListener sentListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_requests_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SentRequestAdapter(requireContext(), requests);
        recyclerView.setAdapter(adapter);

        loadSentRequests();
    }

    private void loadSentRequests() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String safeEmail = user.getEmail().replace(".", "_");
        sentRef = FirebaseDatabase.getInstance().getReference("sentRequests").child(safeEmail);

        sentListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requests.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    SentRequest request = snap.getValue(SentRequest.class);
                    if (request != null) {
                        request.setKey(snap.getKey());
                        requests.add(request);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Error loading sent requests", Toast.LENGTH_SHORT).show();
            }
        };
        sentRef.addValueEventListener(sentListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sentRef != null && sentListener != null) {
            sentRef.removeEventListener(sentListener);
        }
    }
}