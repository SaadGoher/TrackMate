package com.example.trackmate.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.trackmate.R;
import com.example.trackmate.adapters.MessageAdapter;
import com.example.trackmate.models.Message;
import com.example.trackmate.models.ReportedItem;
import com.example.trackmate.models.User;
import com.example.trackmate.services.FirebaseService;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesActivity extends AppCompatActivity {

    private static final String TAG = "MessagesActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText messageInput;
    private ImageButton sendButton, attachButton, callButton;
    private ImageView receiverProfileImage, itemImage;
    private TextView receiverName, itemName, itemDetails;
    private MaterialCardView itemDetailCard;
    
    private String currentUserId;
    private String receiverId;
    private Uri attachedImageUri;
    private ReportedItem relatedItem;
    private User receiver;
    
    // Activity result launcher for image selection
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        // Initialize view components
        initializeViews();
        
        // Set up activity result launcher for image picking
        setupImagePicker();
        
        // Set up click listeners
        setupClickListeners();
        
        // Initialize data
        currentUserId = FirebaseService.getCurrentUser().getUid();
        receiverId = getIntent().getStringExtra("receiver_id");
        
        // Check if there's a related item
        String itemId = getIntent().getStringExtra("item_id");
        if (itemId != null) {
            loadRelatedItem(itemId);
        }
        
        // Load messages and receiver profile
        loadReceiverProfile();
        loadMessages();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            
            // Change the back button color to white
            final Drawable upArrow = getResources().getDrawable(R.drawable.baseline_arrow_back_24);
            upArrow.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
            
            getSupportActionBar().setTitle(""); // We'll show user name in custom title view
        }
        
        // Find views
        recyclerView = findViewById(R.id.recycler_view_messages);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        attachButton = findViewById(R.id.attach_button);
        callButton = findViewById(R.id.call_button);
        receiverProfileImage = findViewById(R.id.receiver_profile_image);
        receiverName = findViewById(R.id.receiver_name);
        
        // Item detail card views
        itemDetailCard = findViewById(R.id.item_detail_card);
        itemImage = findViewById(R.id.item_image);
        itemName = findViewById(R.id.item_name);
        itemDetails = findViewById(R.id.item_details);
        
        // Set up RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from the bottom
        recyclerView.setLayoutManager(layoutManager);
        
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        recyclerView.setAdapter(messageAdapter);
    }
    
    /**
     * Set up activity result launcher for image picking
     */
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        attachedImageUri = imageUri;
                        
                        // Highlight the attach button to show an image is selected
                        attachButton.setColorFilter(getResources().getColor(R.color.primary_dark));
                        
                        // Change send button color to indicate ready to send
                        sendButton.setImageResource(android.R.drawable.ic_menu_send);
                        sendButton.setColorFilter(R.color.primary_dark);
                        
                        Toast.makeText(this, "Image attached. Send your message now.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Reset the attach button color if no image was selected
                    attachButton.setColorFilter(null);
                }
            }
        );
    }
    
    /**
     * Set up click listeners for buttons
     */
    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> sendMessage());
        attachButton.setOnClickListener(v -> attachImage());
        callButton.setOnClickListener(v -> callReceiver());

        // Navigate to user profile on profile image click
        receiverProfileImage.setOnClickListener(v -> {
            if (receiver != null) {
                navigateToUserProfile(receiverId, getDisplayName(receiver));
            } else {
                navigateToUserProfile(receiverId, receiverName.getText().toString());
            }
        });
    }

    /**
     * Send a message with optional image attachment
     */
    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        
        // Check if we have either text or image
        if (TextUtils.isEmpty(messageText) && attachedImageUri == null) {
            Toast.makeText(this, "Cannot send empty message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show sending indicator
        Toast.makeText(this, "Sending message...", Toast.LENGTH_SHORT).show();
        
        // Create a new message
        Message message = new Message();
        message.setSenderId(currentUserId);
        message.setReceiverId(receiverId);
        message.setText(messageText);
        message.setTimestamp(System.currentTimeMillis());
        
        // Add related item info if available
        if (relatedItem != null) {
            message.setRelatedItemId(relatedItem.getId());
            message.setItemName(relatedItem.getName());
            message.setItemImageUrl(relatedItem.getImageUrl());
        }

        // If we have an image, upload it first
        if (attachedImageUri != null) {
            FirebaseService.uploadImage(attachedImageUri, task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    // Set the image URL in the message
                    message.setImageUrl(task.getResult().toString());
                    
                    // Save the message to Firebase
                    saveMessageToFirebase(message);
                } else {
                    Log.e(TAG, "Failed to upload image", task.getException());
                    Toast.makeText(MessagesActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // No image, just save the message
            saveMessageToFirebase(message);
        }
        
        // Clear the input field
        messageInput.setText("");
        
        // Reset attachment
        attachedImageUri = null;
        attachButton.setColorFilter(null);
        
        // Reset send button
        sendButton.setImageResource(android.R.drawable.ic_menu_send);
        sendButton.setColorFilter(null);
        
        // Reset input field background
        messageInput.setBackground(getResources().getDrawable(R.drawable.bg_message_input));
    }
    
    /**
     * Save the message to Firebase
     */
    private void saveMessageToFirebase(Message message) {
        DatabaseReference messageRef = FirebaseService.getDatabase().child("messages").push();
        String messageId = messageRef.getKey();
        
        Map<String, Object> messageValues = message.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/messages/" + messageId, messageValues);
        childUpdates.put("/user-messages/" + currentUserId + "/" + receiverId + "/" + messageId, messageValues);
        childUpdates.put("/user-messages/" + receiverId + "/" + currentUserId + "/" + messageId, messageValues);

        FirebaseService.getDatabase().updateChildren(childUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Message sent successfully");
                // Scroll to the latest message
                scrollToLatestMessage();
            } else {
                Log.e(TAG, "Failed to send message", task.getException());
                Toast.makeText(MessagesActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Load messages from Firebase and setup listener for new messages
     */
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
                
                // Sort messages by timestamp
                Collections.sort(messageList, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));
                
                // Update adapter
                messageAdapter.notifyDataSetChanged();
                
                // Scroll to the bottom if there are messages
                if (messageList.size() > 0) {
                    recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to load messages", error.toException());
                Toast.makeText(MessagesActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReceiverProfile() {
        DatabaseReference userRef = FirebaseService.getDatabase().child("users").child(receiverId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                receiver = snapshot.getValue(User.class);
                if (receiver != null) {
                    // Store the user ID in the receiver object
                    receiver.setId(receiverId);
                    
                    // Set receiver name with proper fallback
                    receiverName.setText(getDisplayName(receiver));
                    
                    // Make sure the text color is visible (white for dark toolbar)
                    receiverName.setTextColor(getResources().getColor(android.R.color.white));
                    
                    // Bold the text for better visibility
                    receiverName.setTypeface(receiverName.getTypeface(), android.graphics.Typeface.BOLD);
                    
                    // Load profile image if available
                    if (receiver.getProfileImageUrl() != null && !receiver.getProfileImageUrl().isEmpty()) {
                        Glide.with(MessagesActivity.this)
                             .load(receiver.getProfileImageUrl())
                             .circleCrop()
                             .placeholder(R.drawable.baseline_person_24)
                             .error(R.drawable.baseline_person_24)
                             .into(receiverProfileImage);
                    } else {
                        // Set a default image
                        receiverProfileImage.setImageResource(R.drawable.baseline_person_24);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to load receiver profile", error.toException());
            }
        });
    }

    /**
     * Handle attachment of images
     */
    private void attachImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Image"));
        
        // Show visual feedback
        attachButton.setColorFilter(getResources().getColor(R.color.primary_dark));
    }
    
    /**
     * Handle calling the receiver
     */
    private void callReceiver() {
        // Get user's phone number from Firebase
        DatabaseReference userRef = FirebaseService.getDatabase().child("users").child(receiverId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                    // Launch phone dialer with the user's number
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + user.getPhoneNumber()));
                    startActivity(intent);
                } else {
                    Toast.makeText(MessagesActivity.this, "No phone number available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to get phone number", error.toException());
                Toast.makeText(MessagesActivity.this, "Failed to retrieve phone number", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadRelatedItem(String itemId) {
        DatabaseReference itemRef = FirebaseService.getDatabase().child("reported_items").child(itemId);
        itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    ReportedItem item = snapshot.getValue(ReportedItem.class);
                    if (item != null) {
                        item.setId(snapshot.getKey());
                        relatedItem = item;
                        displayRelatedItem(item);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading related item", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error loading item: " + error.getMessage(), error.toException());
            }
        });
    }
    
    /**
     * Display the related item in the item card
     */
    private void displayRelatedItem(ReportedItem item) {
        itemDetailCard.setVisibility(View.VISIBLE);
        
        // Set item name and details
        itemName.setText(item.getName());
        String details = item.getType() + " on " + item.getDate() + " at " + item.getLocation();
        itemDetails.setText(details);
        
        // Load item image
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(this)
                 .load(item.getImageUrl())
                 .centerCrop()
                 .into(itemImage);
        } else {
            itemImage.setImageResource(R.drawable.baseline_inventory_24);
        }
        
        // Set click listener to open item details
        itemDetailCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, ItemDetailActivity.class);
            intent.putExtra("item_id", item.getId());
            startActivity(intent);
        });
    }
    
    /**
     * Scroll to the latest message
     */
    private void scrollToLatestMessage() {
        if (messageList.size() > 0) {
            recyclerView.post(() -> {
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up focus change listener for message input to show active state
        messageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                messageInput.setBackground(getResources().getDrawable(R.drawable.bg_message_input_active));
            } else {
                messageInput.setBackground(getResources().getDrawable(R.drawable.bg_message_input));
            }
        });
        
        // Set up text watcher to show active state when typing
        messageInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    messageInput.setBackground(getResources().getDrawable(R.drawable.bg_message_input_active));
                    sendButton.setImageResource(android.R.drawable.ic_menu_send);
                    sendButton.setColorFilter(getResources().getColor(com.google.android.material.R.color.design_default_color_primary));
                } else {
                    messageInput.setBackground(getResources().getDrawable(R.drawable.bg_message_input));
                    sendButton.setImageResource(android.R.drawable.ic_menu_send);
                    sendButton.setColorFilter(null);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void navigateToUserProfile(String userId, String userName) {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
    }
    
    /**
     * Get a valid display name with fallbacks to handle null or empty values
     */
    private String getDisplayName(User user) {
        if (user == null) {
            return "Unknown User";
        }
        
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            return user.getDisplayName();
        } else if (user.getFullName() != null && !user.getFullName().isEmpty()) {
            return user.getFullName();
        } else if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            // Use email but trim the domain part for cleaner display
            String email = user.getEmail();
            int atIndex = email.indexOf('@');
            return atIndex > 0 ? email.substring(0, atIndex) : email;
        } else {
            return "User " + user.getId();
        }
    }
}