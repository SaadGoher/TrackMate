package com.example.trackmate.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.trackmate.R;
import com.example.trackmate.activities.ItemDetailActivity;
import com.example.trackmate.activities.LocationPickerActivity;
import com.example.trackmate.models.ReportedItem;
import com.example.trackmate.services.FirebaseService;
import com.example.trackmate.utils.ImageSimilaritySearch;
import com.example.trackmate.utils.ItemMatcher;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReportFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int LOCATION_PICKER_REQUEST = 2;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 3;

    private ImageView itemImage;
    private TextInputEditText itemName;
    private TextInputEditText itemDate;
    private TextInputEditText itemLocation;
    private TextInputEditText itemDescription;
    private ChipGroup itemStatusGroup;
    private MaterialButton submitButton;
    private MaterialButton selectLocationButton;
    private ProgressBar progressBar;
    private Uri imageUri;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private boolean useCurrentLocation = false;
    private FusedLocationProviderClient fusedLocationClient;

    private boolean isEditMode = false;
    private String editItemId = null;

    public static String TAG = "ReportFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        // Check if we're in edit mode
        Bundle args = getArguments();
        if (args != null) {
            isEditMode = args.getBoolean("EDIT_MODE", false);
            editItemId = args.getString("ITEM_ID");
        }

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        itemImage = view.findViewById(R.id.item_image);
        itemName = view.findViewById(R.id.item_name);
        itemDate = view.findViewById(R.id.item_date);
        itemLocation = view.findViewById(R.id.item_location);
        itemDescription = view.findViewById(R.id.item_description);
        itemStatusGroup = view.findViewById(R.id.item_status_group);
        submitButton = view.findViewById(R.id.submit_button);
        selectLocationButton = view.findViewById(R.id.select_location_button);
        progressBar = view.findViewById(R.id.progress_bar);

        itemImage.setOnClickListener(v -> openFileChooser());
        itemDate.setOnClickListener(v -> showDatePickerDialog());

        // Set up location selection
        itemLocation.setOnClickListener(v -> showLocationOptions());
        selectLocationButton.setOnClickListener(v -> openLocationPicker());

        submitButton.setOnClickListener(v -> submitReport());

        // If in edit mode, load the item data
        if (isEditMode && editItemId != null) {
            loadItemForEditing(editItemId);
            submitButton.setText("Update Item");
        }

        return view;
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year1, month1, dayOfMonth) -> {
            String date = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
            itemDate.setText(date);
        }, year, month, day);
        datePickerDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                // Use scaleType=centerCrop for the preview
                itemImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

                // Remove the tint when an image is selected
                itemImage.setImageTintList(null);

                // Load image with Glide to properly handle the image
                if (getContext() != null) {
                    Glide.with(getContext())
                            .load(imageUri)
                            .centerCrop()
                            .placeholder(R.drawable.baseline_image_24)
                            .error(R.drawable.baseline_broken_image_24)
                            .into(itemImage);
                } else {
                    // Fallback to direct loading if context is null
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                    itemImage.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image preview", e);
                Toast.makeText(getContext(), "Error loading image preview", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PICKER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            // Get location data from picker
            latitude = data.getDoubleExtra("latitude", 0);
            longitude = data.getDoubleExtra("longitude", 0);
            String address = data.getStringExtra("address");

            // Set location text
            if (address != null && !address.isEmpty()) {
                itemLocation.setText(address);
            } else {
                itemLocation.setText("Selected Location (" + latitude + ", " + longitude + ")");
            }

            useCurrentLocation = false;

            // Reset the image view to default state
            itemImage.setScaleType(ImageView.ScaleType.CENTER);
            itemImage.setImageResource(R.drawable.baseline_add_a_photo_24);
            imageUri = null;
        }

    }

    private void submitReport() {
        String name = itemName.getText().toString().trim();
        String date = itemDate.getText().toString().trim();
        String location = itemLocation.getText().toString().trim();
        String description = itemDescription.getText().toString().trim();
        int checkedChipId = itemStatusGroup.getCheckedChipId();

        if (checkedChipId == View.NO_ID) {
            Toast.makeText(getContext(), "Please select Lost or Found", Toast.LENGTH_SHORT).show();
            return;
        }

        String status = ((com.google.android.material.chip.Chip) getView().findViewById(checkedChipId)).getText().toString().trim();

        if (validateInputs(name, date, location, description, status)) {
            // Show progress indicator and disable submit button
            progressBar.setVisibility(View.VISIBLE);
            submitButton.setEnabled(false);

            // Set a toast to indicate processing
            String actionText = isEditMode ? "Updating" : "Reporting";
            Toast.makeText(getContext(), actionText + " your item...", Toast.LENGTH_SHORT).show();

            if (isEditMode && editItemId != null) {
                updateExistingItem(name, date, location, description, status);
            } else {
                // Create new item
                if (imageUri != null) {
                    // First upload the image, then create the report
                    FirebaseService.uploadImage(imageUri, new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                String imageUrl = task.getResult().toString();
                                createAndSaveReport(name, date, location, description, imageUrl, status);
                            } else {
                                handleError("Failed to upload image");
                            }
                        }
                    });
                } else {
                    // Should never reach here due to validation, but as a fallback
                    handleError("An image is required for reporting an item");
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                }
            }
        }
    }
    
    private void updateExistingItem(String name, String date, String location, String description, String status) {
        String userId = FirebaseService.getCurrentUser().getUid();
        ReportedItem.Type type = status.equals("Lost") ? ReportedItem.Type.LOST : ReportedItem.Type.FOUND;
        
        // Create a map of updated values
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("date", date);
        updates.put("location", location);
        updates.put("description", description);
        updates.put("type", type);
        updates.put("typeString", type.toString());
        updates.put("latitude", latitude);
        updates.put("longitude", longitude);
        updates.put("useCurrentLocation", useCurrentLocation);
        
        // If there's a new image, upload it first
        if (imageUri != null) {
            FirebaseService.uploadImage(imageUri, task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String imageUrl = task.getResult().toString();
                    updates.put("imageUrl", imageUrl);
                    performUpdate(updates);
                } else {
                    handleError("Failed to upload new image");
                }
            });
        } else {
            // Update without changing the image
            performUpdate(updates);
        }
    }
    
    private void performUpdate(Map<String, Object> updates) {
        FirebaseService.getDatabase().child("reported_items").child(editItemId)
            .updateChildren(updates)
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                submitButton.setEnabled(true);
                
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Item updated successfully!", Toast.LENGTH_SHORT).show();
                    // Go back to item details
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                } else {
                    handleError("Failed to update item: " + task.getException().getMessage());
                }
            });
    }

    private boolean validateInputs(String name, String date, String location, String description, String status) {
        if (name.isEmpty()) {
            itemName.setError("Item Name is required");
            return false;
        }
        if (date.isEmpty()) {
            itemDate.setError("Date is required");
            return false;
        }
        if (location.isEmpty()) {
            itemLocation.setError("Location is required");
            return false;
        }
        if (description.isEmpty()) {
            itemDescription.setError("Description is required");
            return false;
        }
        if (status.isEmpty()) {
            Toast.makeText(getContext(), "Status is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (imageUri == null && !isEditMode) {
            Toast.makeText(getContext(), "Image is required for reporting an item", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void createAndSaveReport(String name, String date, String location, String description, String imageUrl, String status) {
        String userId = FirebaseService.getCurrentUser().getUid();
        ReportedItem.Type type = status.equals("Lost") ? ReportedItem.Type.LOST : ReportedItem.Type.FOUND;

        // Create a ReportedItem with location information
        ReportedItem item = new ReportedItem(
                name,
                description,
                location,
                date,
                imageUrl,
                type,
                latitude,
                longitude,
                useCurrentLocation
        );
        item.setUserId(userId);

        FirebaseService.reportItem(userId, item, task -> {
            if (task.isSuccessful()) {
                // Now that the item is saved, process the image for similarity search
                if (imageUri != null) {
                    try {
                        // Convert URI to Bitmap for processing
                        Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);

                        if (imageBitmap == null) {
                            // If bitmap is null, just notify success and clear form
                            progressBar.setVisibility(View.GONE);
                            submitButton.setEnabled(true);
                            Toast.makeText(getContext(), "Item reported successfully!", Toast.LENGTH_SHORT).show();
                            clearForm();
                            return;
                        }

                        // Process the image with our ItemMatcher
                        ItemMatcher.processNewItem(getContext(), item, imageBitmap, new ItemMatcher.MatchListener() {
                            @Override
                            public void onMatchesFound(List<ReportedItem> matchedItems, Map<String, Float> similarityScores) {
                                progressBar.setVisibility(View.GONE);
                                submitButton.setEnabled(true);

                                // Show potential matches to the user
                                showMatchesDialog(matchedItems, similarityScores);

                                // Still clear the form since the item has been reported
                                clearForm();
                            }

                            @Override
                            public void onNoMatchesFound() {
                                progressBar.setVisibility(View.GONE);
                                submitButton.setEnabled(true);

                                Toast.makeText(getContext(), "Item reported successfully! No potential matches found.", Toast.LENGTH_LONG).show();
                                clearForm();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                // The item was saved but processing for similarity failed
                                progressBar.setVisibility(View.GONE);
                                submitButton.setEnabled(true);

                                Toast.makeText(getContext(), "Item reported successfully! " + errorMessage, Toast.LENGTH_LONG).show();
                                clearForm();
                            }
                        });

                    } catch (IOException e) {
                        handleError("Item reported but failed to process image: " + e.getMessage());
                        clearForm();
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    Toast.makeText(getContext(), "Item reported successfully!", Toast.LENGTH_SHORT).show();
                    clearForm();
                }
            } else {
                handleError("Failed to report item");
            }
        });
    }

    /**
     * Show a dialog with potential matches
     */
    private void showMatchesDialog(List<ReportedItem> matchedItems, Map<String, Float> similarityScores) {
        if (matchedItems.isEmpty()) {
            return;
        }

        // Build a list of item names with their similarity scores
        String[] itemNames = new String[matchedItems.size()];
        String[] itemIds = new String[matchedItems.size()];

        for (int i = 0; i < matchedItems.size(); i++) {
            ReportedItem item = matchedItems.get(i);
            float score = similarityScores.get(item.getId()) * 100; // Convert to percentage

            itemNames[i] = item.getName() + " (" + String.format("%.1f", score) + "% match)";
            itemIds[i] = item.getId();
        }

        // Show a dialog with the matches
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Potential Matches Found!")
                .setItems(itemNames, (dialog, which) -> {
                    // Open the selected item's detail
                    String selectedItemId = itemIds[which];
                    openItemDetail(selectedItemId);
                })
                .setPositiveButton("Close", null)
                .setMessage("We found some items that might match what you're looking for. Click on an item to view details.")
                .show();
    }

    /**
     * Open the item detail activity for a specific item
     */
    private void openItemDetail(String itemId) {
        Intent intent = new Intent(getContext(), ItemDetailActivity.class);
        intent.putExtra("ITEM_ID", itemId);
        startActivity(intent);
    }

    private void handleError(String message) {
        progressBar.setVisibility(View.GONE);
        submitButton.setEnabled(true);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void clearForm() {
        itemName.setText("");
        itemDate.setText("");
        itemLocation.setText("");
        itemDescription.setText("");
        itemStatusGroup.clearCheck();
        itemImage.setScaleType(ImageView.ScaleType.CENTER);
        itemImage.setImageResource(R.drawable.baseline_add_a_photo_24);
        itemImage.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.black)));
        imageUri = null;
    }

    private void showLocationOptions() {
        String[] options = {"Select on Map", "Use Current Location", "Enter Manually"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Choose Location Method")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openLocationPicker();
                            break;
                        case 1:
                            requestLocationPermission();
                            break;
                        case 2:
                            itemLocation.setFocusableInTouchMode(true);
                            itemLocation.requestFocus();
                            break;
                    }
                })
                .show();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    progressBar.setVisibility(View.GONE);

                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        useCurrentLocation = true;

                        // Use Geocoder to get address
                        try {
                            android.location.Geocoder geocoder = new android.location.Geocoder(requireContext());
                            java.util.List<android.location.Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                            if (addresses != null && addresses.size() > 0) {
                                android.location.Address address = addresses.get(0);
                                StringBuilder sb = new StringBuilder();

                                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                                    sb.append(address.getAddressLine(i));
                                    if (i < address.getMaxAddressLineIndex()) {
                                        sb.append(", ");
                                    }
                                }

                                itemLocation.setText(sb.toString());
                            } else {
                                itemLocation.setText("Current Location (" + latitude + ", " + longitude + ")");
                            }
                        } catch (Exception e) {
                            itemLocation.setText("Current Location (" + latitude + ", " + longitude + ")");
                            Log.e(TAG, "Error getting address: " + e.getMessage());
                        }
                    } else {
                        Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openLocationPicker() {
        Intent intent = new Intent(requireActivity(), LocationPickerActivity.class);
        startActivityForResult(intent, LOCATION_PICKER_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void loadItemForEditing(String itemId) {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseService.getDatabase().child("reported_items").child(itemId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);

                        ReportedItem item = snapshot.getValue(ReportedItem.class);
                        if (item != null) {
                            // Fill form with item data
                            itemName.setText(item.getName());
                            itemDate.setText(item.getDate());
                            itemLocation.setText(item.getLocation());
                            itemDescription.setText(item.getDescription());

                            // Set location data
                            latitude = item.getLatitude();
                            longitude = item.getLongitude();
                            useCurrentLocation = item.isUseCurrentLocation();

                            // Set the appropriate chip
                            if (item.getType() == ReportedItem.Type.LOST) {
                                itemStatusGroup.check(R.id.status_lost);
                            } else {
                                itemStatusGroup.check(R.id.status_found);
                            }

                            // Load the image if available
                            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                                itemImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                itemImage.setImageTintList(null);
                                Glide.with(requireContext())
                                        .load(item.getImageUrl())
                                        .centerCrop()
                                        .into(itemImage);
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to load item data", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}


