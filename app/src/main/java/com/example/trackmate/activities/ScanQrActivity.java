package com.example.trackmate.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.trackmate.R;
import com.example.trackmate.models.User;
import com.example.trackmate.services.FirebaseService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class ScanQrActivity extends AppCompatActivity {

    private DecoratedBarcodeView barcodeView;
    private boolean scanned = false;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        barcodeView = findViewById(R.id.barcode_scanner);
        barcodeView.setStatusText("Scan a QR code");
        
        // Check for camera permission
        checkCameraPermission();
        
        // Handle deep links if this activity was started by a QR code scan
        handleIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }
    
    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        Uri data = intent.getData();
        
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            if ("trackmate".equals(data.getScheme())) {
                // Handle trackmate:// URLs
                if ("user".equals(data.getHost())) {
                    String userId = data.getPathSegments().get(0);
                    lookupUserProfile(userId);
                }
            }
        }
    }
    
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                // Show an explanation to the user
                showPermissionExplanationDialog();
            } else {
                // No explanation needed; request the permission
                requestCameraPermission();
            }
        } else {
            // Permission has already been granted
            initializeCamera();
        }
    }
    
    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Camera Permission Needed")
                .setMessage("This app needs the camera permission to scan QR codes.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestCameraPermission();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(ScanQrActivity.this, "Camera permission is required to scan QR codes", 
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .create()
                .show();
    }
    
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST_CODE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                initializeCamera();
            } else {
                // Permission denied
                Toast.makeText(this, "Camera permission is required to scan QR codes", 
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    private void initializeCamera() {
        // Set up the callback
        barcodeView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null && !scanned) {
                    scanned = true;
                    
                    // Play a beep sound
                    barcodeView.setStatusText("QR Code Detected");
                    
                    // Parse the QR code content
                    String qrContent = result.getText();
                    
                    try {
                        // Try to parse as JSON
                        JSONObject jsonContent = new JSONObject(qrContent);
                        
                        // Check if it's a TrackMate QR code
                        if (jsonContent.has("appId") && 
                            "com.example.trackmate".equals(jsonContent.getString("appId"))) {
                            
                            // Check if it's a user profile or an item
                            if (jsonContent.has("itemId")) {
                                // It's an item QR code
                                String itemId = jsonContent.getString("itemId");
                                navigateToItemDetail(itemId);
                            } else if (jsonContent.has("userId")) {
                                // It's a user profile QR code
                                String userId = jsonContent.getString("userId");
                                lookupUserProfile(userId);
                            } else {
                                // Unknown TrackMate QR code format
                                Toast.makeText(ScanQrActivity.this, 
                                        "Unknown QR code format", 
                                        Toast.LENGTH_LONG).show();
                                resetScanner();
                            }
                        } else if (jsonContent.has("userId")) {
                            // Legacy format with just user data
                            String userId = jsonContent.getString("userId");
                            lookupUserProfile(userId);
                        } else {
                            // JSON but not our format
                            Toast.makeText(ScanQrActivity.this, 
                                    "Invalid QR code format", 
                                    Toast.LENGTH_LONG).show();
                            resetScanner();
                        }
                    } catch (JSONException e) {
                        // Not a JSON object, check for legacy format
                        if (qrContent.startsWith("trackmateuser:")) {
                            String userId = qrContent.substring("trackmateuser:".length());
                            lookupUserProfile(userId);
                        } else if (qrContent.startsWith("trackmateitem:")) {
                            String itemId = qrContent.substring("trackmateitem:".length());
                            navigateToItemDetail(itemId);
                        } else {
                            // Try to handle as URL
                            handleExternalQrCode(qrContent);
                        }
                    }
                }
            }
        });
    }
    
    private void handleExternalQrCode(String content) {
        // Check if content is a URL
        if (content.startsWith("http://") || content.startsWith("https://")) {
            // Open URL in browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(content));
            startActivity(browserIntent);
            finish();
        } else {
            // Not a URL, just display the content
            Toast.makeText(this, "Scanned: " + content, Toast.LENGTH_LONG).show();
            resetScanner();
        }
    }
    
    private void resetScanner() {
        scanned = false;
        barcodeView.resume();
    }
    
    private void navigateToItemDetail(String itemId) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("item_id", itemId);
        startActivity(intent);
        finish();
    }
    
    private void addFriend(String userId) {
        String currentUserId = FirebaseService.getCurrentUser().getUid();
        
        // Don't add yourself as a friend
        if (currentUserId.equals(userId)) {
            Toast.makeText(this, "You cannot add yourself as a friend", Toast.LENGTH_SHORT).show();
            resetScanner();
            return;
        }
        
        // Add bidirectional friendship
        FirebaseService.getDatabase().child("friendships").child(currentUserId).child(userId).setValue(true)
            .addOnSuccessListener(aVoid -> {
                FirebaseService.getDatabase().child("friendships").child(userId).child(currentUserId).setValue(true)
                    .addOnSuccessListener(aVoid2 -> {
                        Toast.makeText(this, "Friend added successfully", Toast.LENGTH_SHORT).show();
                        lookupUserProfile(userId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error adding friend", Toast.LENGTH_SHORT).show();
                        resetScanner();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error adding friend", Toast.LENGTH_SHORT).show();
                resetScanner();
            });
    }

    private void lookupUserProfile(String userId) {
        DatabaseReference userRef = FirebaseService.getDatabase()
                .child("users").child(userId);
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        // Navigate to user profile
                        navigateToProfile(userId, user.getDisplayName());
                    } else {
                        showError("User data could not be read.");
                    }
                } else {
                    showError("User not found.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showError("Database error: " + databaseError.getMessage());
            }
        });
    }
    
    private void navigateToProfile(String userId, String userName) {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
        finish();
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only resume if we have permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}
