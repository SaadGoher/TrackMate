package com.example.trackmate.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.trackmate.R;
import com.example.trackmate.adapters.ConversationAdapter;
import com.example.trackmate.adapters.MessageAdapter;
import com.example.trackmate.models.Message;
import com.example.trackmate.services.FirebaseService;
import com.example.trackmate.utils.SharedPrefsUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import com.example.trackmate.MainActivity;
import com.example.trackmate.models.User;
import com.example.trackmate.activities.MessagesActivity;
import android.content.Intent;
import java.util.HashSet;
import java.util.Set;

import java.util.List;
import androidx.recyclerview.widget.DividerItemDecoration;

public class MessageFragment extends Fragment {


    private RecyclerView recyclerView;
    private ConversationAdapter messageAdapter; // Change type to ConversationAdapter
    private ArrayList<Message> messageList;
    private String currentUserId;
    private Set<String> conversationUserIds;

    public MessageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        messageList = new ArrayList<>(); // Initialize the list here
        conversationUserIds = new HashSet<>();
        currentUserId = SharedPrefsUtil.getUserId(requireContext()); // Get current user ID
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);
        
        // Set toolbar title and back button
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.showBackButton(true);
            activity.setToolbarTitle("Messages");
        }

        setupRecyclerView(view);
        loadMessages();

        return view;
    }

    private void loadMessages() {
        DatabaseReference userMessagesRef = FirebaseService.getDatabase()
                .child("user-messages")
                .child(currentUserId);

        userMessagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                conversationUserIds.clear();
                
                // Collect all unique user IDs from conversations
                for (DataSnapshot conversationSnapshot : dataSnapshot.getChildren()) {
                    String userId = conversationSnapshot.getKey();
                    if (userId != null && !userId.equals(currentUserId)) {
                        conversationUserIds.add(userId);
                    }
                }
                
                // Load user details for each conversation
                loadUserDetails();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    private void loadUserDetails() {
        messageList.clear();
        for (String userId : conversationUserIds) {
            DatabaseReference userRef = FirebaseService.getDatabase()
                    .child("users")
                    .child(userId);
            
            // Get user details
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // Get last message for this conversation
                        Query messageQuery = FirebaseService.getDatabase()
                                .child("user-messages")
                                .child(currentUserId)
                                .child(userId)
                                .limitToLast(1);
                                
                        DatabaseReference messageRef = (DatabaseReference) messageQuery.getRef();
                        
                        messageRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot messageSnapshot) {
                                Message conversation = new Message();
                                conversation.setReceiverId(userId);
                                conversation.setReceiverName(user.getDisplayName());
                                conversation.setReceiverImage(user.getProfileImageUrl()); // Update to match User model field name
                                
                                if (messageSnapshot.exists()) {
                                    for (DataSnapshot child : messageSnapshot.getChildren()) {
                                        Message lastMessage = child.getValue(Message.class);
                                        if (lastMessage != null) {
                                            conversation.setText(lastMessage.getText());
                                            conversation.setTimestamp(lastMessage.getTimestamp());
                                        }
                                    }
                                }
                                
                                messageList.add(conversation);
                                messageAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                // Handle error
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle error
                }
            });
        }
    }

    // Add click handler for the RecyclerView items
    private void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        
        messageAdapter = new ConversationAdapter(messageList, userId -> {
            Intent intent = new Intent(getContext(), MessagesActivity.class);
            intent.putExtra("receiver_id", userId);
            startActivity(intent);
        });
        recyclerView.setAdapter(messageAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Reset toolbar when leaving fragment
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.showBackButton(false);
            activity.setToolbarTitle("TrackMate");
        }
    }
}