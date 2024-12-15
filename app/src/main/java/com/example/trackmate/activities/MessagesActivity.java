package com.example.trackmate.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackmate.R;
import com.example.trackmate.adapters.MessageAdapter;
import com.example.trackmate.models.Message;
import com.example.trackmate.models.User;
import com.example.trackmate.services.FirebaseService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesActivity extends AppCompatActivity {

    private static final String TAG = "MessagesActivity";

    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText messageInput;
    private ImageButton sendButton, attachButton;
    private String currentUserId;
    private String receiverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chat");
        }

        recyclerView = findViewById(R.id.recycler_view_messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();
        currentUserId = FirebaseService.getCurrentUser().getUid();
        receiverId = getIntent().getStringExtra("receiver_id");
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        recyclerView.setAdapter(messageAdapter);

        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        attachButton = findViewById(R.id.attach_button);

        sendButton.setOnClickListener(v -> sendMessage());
        attachButton.setOnClickListener(v -> attachImage());

        loadReceiverProfile();
        loadMessages();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (!TextUtils.isEmpty(messageText)) {
            DatabaseReference messageRef = FirebaseService.getDatabase().child("messages").push();
            String messageId = messageRef.getKey();
            Message message = new Message();
            message.setSenderId(currentUserId);
            message.setReceiverId(receiverId);
            message.setText(messageText);
            message.setImageUrl(null); // Assuming no image for now
            message.setTimestamp(System.currentTimeMillis());

            Map<String, Object> messageValues = message.toMap();
            Map<String, Object> childUpdates = new HashMap<>();
            childUpdates.put("/messages/" + messageId, messageValues);
            childUpdates.put("/user-messages/" + currentUserId + "/" + receiverId + "/" + messageId, messageValues);
            childUpdates.put("/user-messages/" + receiverId + "/" + currentUserId + "/" + messageId, messageValues);

            FirebaseService.getDatabase().updateChildren(childUpdates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Message sent successfully");
                } else {
                    Log.e(TAG, "Failed to send message", task.getException());
                }
            });
            messageInput.setText("");
        }
    }

    private void loadMessages() {
        DatabaseReference messagesRef = FirebaseService.getDatabase().child("user-messages").child(currentUserId).child(receiverId);
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        messageList.add(message);
                    }
                }
                messageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to load messages", error.toException());
            }
        });
    }

    private void loadReceiverProfile() {
        DatabaseReference userRef = FirebaseService.getDatabase().child("users").child(receiverId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(user.getDisplayName()); // Ensure User class has getDisplayName() method
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to load receiver profile", error.toException());
            }
        });
    }

    private void attachImage() {
        // Handle image attachment
    }
}