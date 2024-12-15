package com.example.trackmate.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.trackmate.R;
import com.example.trackmate.adapters.MessageAdapter;
import com.example.trackmate.models.Message;

import java.util.List;
public class MessageFragment extends Fragment {


    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    public MessageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);
        recyclerView = view.findViewById(R.id.recycler_view_messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        messageAdapter = new MessageAdapter(messageList, "currentUserId"); // Replace with actual current user ID
        recyclerView.setAdapter(messageAdapter);
        loadMessages();
        return view;
    }

    private void loadMessages() {
        // Load messages from the database and update messageList
        // messageAdapter.notifyDataSetChanged();
    }
}