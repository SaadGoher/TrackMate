package com.example.trackmate.activities;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.trackmate.R;
import com.example.trackmate.adapters.SimilarItemAdapter;
import com.example.trackmate.fragments.ReportFragment;
import com.example.trackmate.models.ImageEmbedding;
import com.example.trackmate.models.ReportedItem;
import com.example.trackmate.models.User;
import com.example.trackmate.services.FirebaseService;
import com.example.trackmate.utils.ImageSimilaritySearch;
import com.example.trackmate.utils.ItemMatcher;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ItemDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "ItemDetailActivity";
    
    private ImageView itemImage;
    private TextView itemName, itemDate, itemLocation, itemDescription;
    private ImageView ownerImage;
    private TextView ownerName, ownerInfo;
    private TextView similarityScore;
    private MaterialButton callButton, messageButton, editButton, shareQrButton, deleteButton, statusButton;
    private RecyclerView similarItemsRecyclerView;
    private TextView similarItemsTitle;
    private MaterialCardView similarityCard, similarItemsCard, mapCard;
    private ProgressBar progressBar, mapLoadingProgress;
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    
    private SimilarItemAdapter similarItemsAdapter;
    private List<ReportedItem> similarItems = new ArrayList<>();
    private Map<String, Float> similarityScores = new HashMap<>();
    
    private ReportedItem currentItem;
    private String ownerId;
    private Bitmap qrBitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Item Details");

        // Set the back arrow icon instead of hamburger
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24);
        
        // Make toolbar icons white
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        
        // Set white color for the back button
        Drawable navigationIcon = toolbar.getNavigationIcon();
        if (navigationIcon != null) {
            navigationIcon.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
        }

        // Initialize views
        itemImage = findViewById(R.id.item_image);
        itemName = findViewById(R.id.item_name);
        itemDate = findViewById(R.id.item_date);
        itemLocation = findViewById(R.id.item_location);
        itemDescription = findViewById(R.id.item_description);
        
        // Owner information views
        ownerImage = findViewById(R.id.owner_image);
        ownerName = findViewById(R.id.owner_name);
        ownerInfo = findViewById(R.id.owner_info);
        
        // Similarity information views
        similarityCard = findViewById(R.id.similarity_card);
        similarityScore = findViewById(R.id.similarity_score);
        
        // Similar items views
        similarItemsCard = findViewById(R.id.similar_items_card);
        similarItemsRecyclerView = findViewById(R.id.similar_items_recycler_view);
        similarItemsTitle = findViewById(R.id.similar_items_title);
        progressBar = findViewById(R.id.progress_bar);
        
        // Map view
        mapCard = findViewById(R.id.map_card);
        mapLoadingProgress = findViewById(R.id.map_loading);
        
        // Initialize map fragment
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.item_location_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        // Action buttons
        callButton = findViewById(R.id.call_button);
        messageButton = findViewById(R.id.message_button);
        editButton = findViewById(R.id.edit_button);
        deleteButton = findViewById(R.id.delete_button);
        statusButton = findViewById(R.id.status_button);
        shareQrButton = findViewById(R.id.share_qr_button);
        shareQrButton.setOnClickListener(v -> generateAndShareItemQrCode());

        // Setup RecyclerView
        similarItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        similarItemsAdapter = new SimilarItemAdapter(this, similarItems, similarityScores, 
                item -> {
                    // When a similar item is clicked, open its detail page
                    Intent intent = new Intent(ItemDetailActivity.this, ItemDetailActivity.class);
                    intent.putExtra("item_id", item.getId());
                    startActivity(intent);
                });
        similarItemsRecyclerView.setAdapter(similarItemsAdapter);

        String itemId = getIntent().getStringExtra("item_id");
        if (itemId != null) {
            loadItemDetails(itemId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        // Enable basic UI settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        
        // If we already have the item loaded, show its location on the map
        if (currentItem != null && currentItem.getLatitude() != 0 && currentItem.getLongitude() != 0) {
            showItemLocationOnMap();
        }
    }
    
    private void showItemLocationOnMap() {
        if (mMap == null || currentItem == null) {
            Log.d(TAG, "Map or current item is null");
            return;
        }
        
        // Hide loading indicator
        if (mapLoadingProgress != null) {
            mapLoadingProgress.setVisibility(View.GONE);
        }
        
        // Get the item's location
        double lat = currentItem.getLatitude();
        double lng = currentItem.getLongitude();
        
        Log.d(TAG, "Item location: " + lat + ", " + lng);
        
        // If location is available (not 0,0), show it on the map
        if (lat != 0 && lng != 0) {
            LatLng itemLocation = new LatLng(lat, lng);
            
            // Add a marker for the item location
            mMap.clear(); // Clear any existing markers
            mMap.addMarker(new MarkerOptions()
                    .position(itemLocation)
                    .title(currentItem.getName()));
            
            // Move camera to the location with zoom
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(itemLocation, 15f));
            
            // Show the map card
            if (mapCard != null) {
                mapCard.setVisibility(View.VISIBLE);
            }
        } else {
            Log.d(TAG, "Item has no valid location coordinates");
            // Hide map card if no valid location
            if (mapCard != null) {
                mapCard.setVisibility(View.GONE);
            }
        }
    }
    
    private void loadItemDetails(String itemId) {
        // Show progress indicator
        progressBar.setVisibility(View.VISIBLE);
        
        // Set a timeout to hide the progress bar after 10 seconds if it's still visible
        progressBar.postDelayed(() -> {
            if (progressBar.getVisibility() == View.VISIBLE) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ItemDetailActivity.this, 
                        "Taking longer than expected. Please check your connection.", Toast.LENGTH_SHORT).show();
            }
        }, 10000); // 10 seconds timeout
        
        DatabaseReference itemRef = FirebaseService.getDatabase().child("reported_items").child(itemId);
        itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    // Hide progress indicator
                    progressBar.setVisibility(View.GONE);
                    
                    ReportedItem item = snapshot.getValue(ReportedItem.class);
                    if (item != null) {
                        item.setId(snapshot.getKey());
                        currentItem = item;
                        displayItemDetails(item);
                        loadOwnerDetails(item.getUserId());
                        
                        // If status is not set, default to OPEN
                        if (currentItem.getStatus() == null) {
                            currentItem.setStatus(ReportedItem.Status.OPEN);
                        }
                        
                        // Get the similarity score if passed from a previous screen
                        Float score = getIntent().getFloatExtra("similarity_score", -1);
                        if (score > 0) {
                            displaySimilarityScore(score);
                        }
                        
                        // Find similar items
                        findSimilarItems(item);
                        
                        // Show item location on map
                        showItemLocationOnMap();
                    } else {
                        Toast.makeText(ItemDetailActivity.this, 
                                "Item not found", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    // Hide progress indicator in case of error
                    progressBar.setVisibility(View.GONE);
                    
                    Log.e(TAG, "Error loading item details", e);
                    Toast.makeText(ItemDetailActivity.this, 
                            "Error loading item details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Hide progress indicator in case of error
                progressBar.setVisibility(View.GONE);
                
                Log.e(TAG, "Firebase error: " + error.getMessage());
                Toast.makeText(ItemDetailActivity.this, 
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayItemDetails(ReportedItem item) {
        itemName.setText(item.getName());
        itemDate.setText(item.getDate());
        itemLocation.setText(item.getLocation());
        itemDescription.setText(item.getDescription());
        
        // Append status to the item name
        if (item.getStatus() == ReportedItem.Status.CLOSED) {
            itemName.setText(itemName.getText() + " [CLOSED]");
        }
        
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(this).load(item.getImageUrl()).into(itemImage);
        } else {
            itemImage.setImageResource(R.drawable.item);
        }

        // Check if current user is the owner of this item
        String currentUserId = FirebaseService.getCurrentUser().getUid();
        if (currentUserId != null && currentUserId.equals(item.getUserId())) {
            // Show edit, status, and delete buttons only for the owner
            editButton.setVisibility(View.VISIBLE);
            statusButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            
            // Configure status button based on current item status
            updateStatusButtonAppearance(item.getStatus());
            
            // Set up edit button
            editButton.setOnClickListener(v -> {
                // Launch edit activity with the current item
                openReportFragmentForEdit(item);
            });
            
            // Set up status toggle button
            statusButton.setOnClickListener(v -> {
                // Toggle item status between OPEN and CLOSED
                toggleItemStatus(item);
            });
            
            // Set up delete button
            deleteButton.setOnClickListener(v -> {
                // Show confirmation dialog before deleting
                showDeleteConfirmationDialog(item);
            });
        } else {
            // Hide owner-only buttons for non-owners
            editButton.setVisibility(View.GONE);
            statusButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        }

        callButton.setOnClickListener(v -> {
            if (item.getUserId() != null) {
                // Get user's phone number
                DatabaseReference userRef = FirebaseService.getDatabase()
                    .child("users").child(item.getUserId());
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(Uri.parse("tel:" + user.getPhoneNumber()));
                            startActivity(intent);
                        } else {
                            Toast.makeText(ItemDetailActivity.this, 
                                    "No phone number available for this user", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ItemDetailActivity.this, 
                                "Failed to get user details", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        messageButton.setOnClickListener(v -> {
            if (item.getUserId() != null) {
                Intent intent = new Intent(this, MessagesActivity.class);
                intent.putExtra("receiver_id", item.getUserId());
                intent.putExtra("item_id", item.getId());
                startActivity(intent);
            }
        });
    }
    
    /**
     * Update the status button appearance based on the current item status
     * 
     * @param status Current status of the item
     */
    private void updateStatusButtonAppearance(ReportedItem.Status status) {
        if (status == ReportedItem.Status.OPEN) {
            statusButton.setText("Mark as Closed");
            statusButton.setIcon(getDrawable(R.drawable.baseline_check_circle_24));
        } else {
            statusButton.setText("Mark as Open");
            statusButton.setIcon(getDrawable(R.drawable.baseline_radio_button_unchecked_24));
        }
    }
    
    /**
     * Toggle the status of the item between OPEN and CLOSED
     */
    private void toggleItemStatus(ReportedItem item) {
        if (item.getId() == null) {
            Toast.makeText(this, "Unable to update item status", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create a database reference to the item
        DatabaseReference itemRef = FirebaseService.getDatabase().child("reported_items").child(item.getId());
        
        // Toggle status between OPEN and CLOSED
        ReportedItem.Status newStatus = (item.getStatus() == ReportedItem.Status.OPEN) 
                ? ReportedItem.Status.CLOSED 
                : ReportedItem.Status.OPEN;
        
        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Updating Status")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        progressDialog.show();
        
        // Update the status in Firebase
        itemRef.child("status").setValue(newStatus).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            
            if (task.isSuccessful()) {
                // Update local item status
                currentItem.setStatus(newStatus);
                
                // Update button appearance
                updateStatusButtonAppearance(newStatus);
                
                // Show success message
                String statusText = (newStatus == ReportedItem.Status.OPEN) ? "open" : "closed";
                Toast.makeText(this, "Item marked as " + statusText, Toast.LENGTH_SHORT).show();
            } else {
                // Show error message
                Toast.makeText(this, "Failed to update status: " + task.getException().getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Show confirmation dialog before deleting an item
     */
    private void showDeleteConfirmationDialog(ReportedItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteItem(item))
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Delete the item from Firebase
     */
    private void deleteItem(ReportedItem item) {
        if (item.getId() == null) {
            Toast.makeText(this, "Unable to delete item", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Deleting Item")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        progressDialog.show();
        
        // Delete from reported_items
        DatabaseReference itemRef = FirebaseService.getDatabase().child("reported_items").child(item.getId());
        itemRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Also delete the embedding if it exists
                DatabaseReference embeddingRef = FirebaseService.getDatabase().child("image_embeddings").child(item.getId());
                embeddingRef.removeValue().addOnCompleteListener(embTask -> {
                    progressDialog.dismiss();
                    
                    // Return to previous screen regardless of embedding deletion result
                    Toast.makeText(this, "Item deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to delete item: " + task.getException().getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadOwnerDetails(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        ownerId = userId;
        DatabaseReference userRef = FirebaseService.getDatabase().child("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        displayOwnerDetails(user);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading owner details", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error loading owner: " + error.getMessage());
            }
        });
    }
    
    private void displayOwnerDetails(User user) {
        // Display owner name
        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = user.getFullName();
        }
        ownerName.setText(displayName);
        
        // Display owner contact info
        String contactInfo = user.getContact();
        if (contactInfo == null || contactInfo.isEmpty()) {
            contactInfo = user.getEmail();
        }
        ownerInfo.setText(contactInfo);
        
        // Load profile image if available
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.baseline_person_24)
                .into(ownerImage);
        }
    }
    
    private void displaySimilarityScore(float score) {
        // Show the similarity card
        similarityCard.setVisibility(View.VISIBLE);
        
        // Format the score as a percentage
        int percentage = Math.round(score * 100);
        similarityScore.setText(String.format(Locale.getDefault(), "%d%% match", percentage));
    }
    
    private void findSimilarItems(ReportedItem item) {
        // Show progress bar and similar items card
        similarItemsCard.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        
        // Set a timeout to hide the progress bar after 10 seconds if it's still visible
        progressBar.postDelayed(() -> {
            if (progressBar.getVisibility() == View.VISIBLE) {
                progressBar.setVisibility(View.GONE);
                if (similarItems.isEmpty()) {
                    // Show a message if no items were found
                    Toast.makeText(ItemDetailActivity.this, "No similar items found", Toast.LENGTH_SHORT).show();
                }
            }
        }, 10000); // 10 seconds timeout
        
        // Get opposite type to search for (if LOST, search for FOUND and vice versa)
        ReportedItem.Type oppositeType = (item.getType() == ReportedItem.Type.LOST) 
                ? ReportedItem.Type.FOUND 
                : ReportedItem.Type.LOST;
        
        // First, get the embedding for the current item
        DatabaseReference embeddingsRef = FirebaseService.getDatabase().child("image_embeddings");
        embeddingsRef.orderByChild("itemId").equalTo(item.getId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            if (!snapshot.exists()) {
                                return;
                            }
                            
                            // Get the current item's embedding
                            ImageEmbedding currentEmbedding = null;
                            for (DataSnapshot embeddingSnapshot : snapshot.getChildren()) {
                                try {
                                    String itemId = embeddingSnapshot.child("itemId").getValue(String.class);
                                    if (item.getId().equals(itemId)) {
                                        Object embeddingObj = embeddingSnapshot.child("embedding").getValue();
                                        if (embeddingObj instanceof ArrayList) {
                                            ArrayList<?> embeddingList = (ArrayList<?>) embeddingObj;
                                            float[] embeddingArray = new float[embeddingList.size()];
                                            
                                            for (int i = 0; i < embeddingList.size(); i++) {
                                                Object value = embeddingList.get(i);
                                                if (value instanceof Double) {
                                                    embeddingArray[i] = ((Double) value).floatValue();
                                                } else if (value instanceof Long) {
                                                    embeddingArray[i] = ((Long) value).floatValue();
                                                } else if (value instanceof Float) {
                                                    embeddingArray[i] = (Float) value;
                                                } else if (value instanceof Integer) {
                                                    embeddingArray[i] = ((Integer) value).floatValue();
                                                }
                                            }
                                    
                                            currentEmbedding = new ImageEmbedding();
                                            currentEmbedding.setItemId(itemId);
                                            currentEmbedding.setEmbedding(embeddingArray);
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing embedding", e);
                                }
                            }
                            
                            if (currentEmbedding != null && currentEmbedding.getEmbedding() != null) {
                                // Now get all items of the opposite type and compare embeddings
                                ImageEmbedding finalCurrentEmbedding = currentEmbedding;
                                FirebaseService.getImageEmbeddingsByType(oppositeType, new FirebaseService.EmbeddingsCallback() {
                                    @Override
                                    public void onEmbeddingsLoaded(List<ImageEmbedding> embeddings) {
                                        try {
                                            if (embeddings.isEmpty()) {
                                                return;
                                            }

                                            List<ItemMatcher.MatchResult> matches = new ArrayList<>();

                                            // Calculate similarities
                                            for (ImageEmbedding otherEmbedding : embeddings) {
                                                if (otherEmbedding.getEmbedding() != null) {
                                                    float similarity = ImageSimilaritySearch.calculateSimilarity(
                                                            finalCurrentEmbedding.getEmbedding(),
                                                            otherEmbedding.getEmbedding());
                                                    
                                                    // Add to matches if similarity is above threshold
                                                    if (similarity >= 0.3f) { // Lower threshold for showing more results
                                                        matches.add(new ItemMatcher.MatchResult(
                                                                otherEmbedding.getItemId(), similarity));
                                                    }
                                                }
                                            }
                                            
                                            if (!matches.isEmpty()) {
                                                // Sort matches by similarity score
                                                matches.sort((a, b) -> 
                                                        Float.compare(b.getSimilarityScore(), a.getSimilarityScore()));
                                                
                                                // Load and display the matching items
                                                loadMatchingItems(matches);
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error processing embeddings", e);
                                        }
                                    }
                                    
                                    @Override
                                    public void onError(String errorMessage) {
                                        Log.e(TAG, "Error getting embeddings: " + errorMessage);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error finding similar items", e);
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Firebase error: " + error.getMessage());
                    }
                });
    }
    
    private void loadMatchingItems(List<ItemMatcher.MatchResult> matches) {
        // Clear previous results
        similarItems.clear();
        similarityScores.clear();
        
        // Store similarity scores
        for (ItemMatcher.MatchResult match : matches) {
            similarityScores.put(match.getItemId(), match.getSimilarityScore());
        }
        
        // Configure title based on item type
        String itemType = currentItem.getType() == ReportedItem.Type.LOST ? "found" : "lost";
        similarItemsTitle.setText("Similar " + itemType + " items");
        
        // Load details for each matching item
        for (ItemMatcher.MatchResult match : matches) {
            FirebaseService.getDatabase()
                    .child("reported_items")
                    .child(match.getItemId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            try {
                                ReportedItem item = snapshot.getValue(ReportedItem.class);
                                if (item != null) {
                                    item.setId(match.getItemId());
                                    similarItems.add(item);
                                    
                                    // Update the adapter
                                    similarItemsAdapter.notifyDataSetChanged();
                                    
                                    // Hide progress bar once we have at least some items
                                    progressBar.setVisibility(View.GONE);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error loading matching item", e);
                            }
                        }
                        
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Firebase error loading match: " + error.getMessage());
                            progressBar.setVisibility(View.GONE);
                        }
                    });
        }
        
        // If no matches, hide progress bar
        if (matches.isEmpty()) {
            progressBar.setVisibility(View.GONE);
        }
    }
    
    private void openReportFragmentForEdit(ReportedItem item) {
        // Create the fragment
        ReportFragment fragment = new ReportFragment();
        
        // Set up the arguments for edit mode
        Bundle args = new Bundle();
        args.putBoolean("EDIT_MODE", true);
        args.putString("ITEM_ID", item.getId());
        fragment.setArguments(args);
        
        // Show the fragment
        getSupportFragmentManager().beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit();
    }
    
    private void generateAndShareItemQrCode() {
        if (currentItem == null) {
            Toast.makeText(this, "Item details not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Create JSON object with item information
            JSONObject itemData = new JSONObject();
            
            // Add app identifiers
            itemData.put("appId", "com.example.trackmate");
            itemData.put("itemId", currentItem.getId());
            
            // Add basic item data
            itemData.put("name", currentItem.getName());
            itemData.put("type", currentItem.getType().toString());
            
            // Add timestamp
            itemData.put("timestamp", System.currentTimeMillis());
            
            // Generate QR code
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(itemData.toString(), BarcodeFormat.QR_CODE, 512, 512);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            qrBitmap = barcodeEncoder.createBitmap(bitMatrix);
            
            // Show dialog to save or share
            showQrCodeActionDialog();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error generating QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showQrCodeActionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("QR Code Generated");
        
        // Inflate a custom view for the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_qr_code, null);
        ImageView qrImageView = dialogView.findViewById(R.id.qr_code_image);
        qrImageView.setImageBitmap(qrBitmap);
        
        builder.setView(dialogView);
        
        // Add buttons for actions
        builder.setPositiveButton("Save", (dialog, which) -> saveQrCode());
        builder.setNegativeButton("Share", (dialog, which) -> shareQrCode());
        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        builder.show();
    }
    
    private void saveQrCode() {
        if (qrBitmap == null) return;

        try {
            String fileName = "trackmate_item_" + currentItem.getId() + "_" + System.currentTimeMillis() + ".png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(imageUri)) {
                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    Toast.makeText(this, "QR code saved to gallery", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void shareQrCode() {
        if (qrBitmap == null) return;

        try {
            String fileName = "trackmate_item_" + currentItem.getId() + "_" + System.currentTimeMillis() + ".png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(imageUri)) {
                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "TrackMate item: " + currentItem.getName());
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Scan this QR code to view details about " + 
                        currentItem.getType().toString().toLowerCase() + " item: " + currentItem.getName());
                
                startActivity(Intent.createChooser(shareIntent, "Share Item QR Code"));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

