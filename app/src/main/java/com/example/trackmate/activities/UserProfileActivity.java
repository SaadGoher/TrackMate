package com.example.trackmate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.trackmate.R;
import com.example.trackmate.adapters.ReportedItemAdapter;
import com.example.trackmate.models.ReportedItem;
import com.example.trackmate.models.User;
import com.example.trackmate.services.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView userName, userEmail, userPhone;
    private MaterialButton callButton, messageButton;
    private RecyclerView reportedItemsList;
    private ReportedItemAdapter itemsAdapter;
    private List<ReportedItem> userItems;
    
    private String userId;
    private User userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("User Profile");
        }

        // Init UI elements
        profileImage = findViewById(R.id.profile_image);
        userName = findViewById(R.id.user_name);
        userEmail = findViewById(R.id.user_email);
        userPhone = findViewById(R.id.user_phone);
        callButton = findViewById(R.id.call_button);
        messageButton = findViewById(R.id.message_button);
        reportedItemsList = findViewById(R.id.reported_items_list);

        // Get user ID from intent
        userId = getIntent().getStringExtra("USER_ID");
        if (userId == null) {
            Toast.makeText(this, "User information not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set temporary user name from intent if available
        String tempName = getIntent().getStringExtra("USER_NAME");
        if (tempName != null && !tempName.isEmpty()) {
            userName.setText(tempName);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(tempName);
            }
        }

        // Setup RecyclerView
        reportedItemsList.setLayoutManager(new GridLayoutManager(this, 2));
        userItems = new ArrayList<>();
        itemsAdapter = new ReportedItemAdapter(this, userItems, item -> {
            Intent intent = new Intent(this, ItemDetailActivity.class);
            intent.putExtra("item_id", item.getId());
            startActivity(intent);
        });
        reportedItemsList.setAdapter(itemsAdapter);

        // Setup click listeners
        callButton.setOnClickListener(v -> callUser());
        messageButton.setOnClickListener(v -> messageUser());

        // Load user profile and items
        loadUserProfile();
        loadUserItems();
    }

    private void loadUserProfile() {
        DatabaseReference userRef = FirebaseService.getDatabase().child("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userProfile = snapshot.getValue(User.class);
                if (userProfile != null) {
                    displayUserInfo(userProfile);
                } else {
                    Toast.makeText(UserProfileActivity.this, 
                            "Could not load user profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserProfileActivity.this, 
                        "Error loading profile: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserInfo(User user) {
        // Set user name
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            userName.setText(user.getDisplayName());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(user.getDisplayName());
            }
        }
        
        // Set user email
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            userEmail.setText(user.getEmail());
            userEmail.setVisibility(View.VISIBLE);
        } else {
            userEmail.setVisibility(View.GONE);
        }
        
        // Set user phone
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            userPhone.setText(user.getPhoneNumber());
            userPhone.setVisibility(View.VISIBLE);
        } else {
            userPhone.setVisibility(View.GONE);
        }
        
        // Load profile image
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .into(profileImage);
        }
        
        // Enable/disable call button based on phone availability
        callButton.setEnabled(user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty());
    }

    private void loadUserItems() {
        Query query = FirebaseService.getDatabase().child("reported_items")
                .orderByChild("userId")
                .equalTo(userId);
                
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userItems.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    ReportedItem item = itemSnapshot.getValue(ReportedItem.class);
                    if (item != null) {
                        item.setId(itemSnapshot.getKey());
                        userItems.add(item);
                    }
                }
                itemsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserProfileActivity.this, 
                        "Error loading items: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void callUser() {
        if (userProfile != null && userProfile.getPhoneNumber() != null 
                && !userProfile.getPhoneNumber().isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + userProfile.getPhoneNumber()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
        }
    }

    private void messageUser() {
        Intent intent = new Intent(this, MessagesActivity.class);
        intent.putExtra("receiver_id", userId);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
