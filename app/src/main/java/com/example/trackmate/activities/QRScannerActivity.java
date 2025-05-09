package com.example.trackmate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.trackmate.R;
import com.example.trackmate.models.User;
import com.example.trackmate.services.FirebaseService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class QRScannerActivity extends AppCompatActivity {
    private DecoratedBarcodeView barcodeView;
    private ProgressBar progressBar;
    private boolean isScanned = false;

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() == null || isScanned) {
                return;
            }
            
            isScanned = true;
            progressBar.setVisibility(View.VISIBLE);
            
            // Process the QR code content
            processQRContent(result.getText());
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            // Not needed for our implementation
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Scan QR Code");
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24);

        // Initialize views
        barcodeView = findViewById(R.id.barcode_scanner);
        progressBar = findViewById(R.id.progress_bar);
        
        // Start scanning
        barcodeView.decodeContinuous(callback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isScanned = false;
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void processQRContent(String content) {
        try {
            // Try to parse as JSON to determine if it's a TrackMate QR code
            JSONObject jsonContent = new JSONObject(content);
            
            if (jsonContent.has("userId") && jsonContent.has("action")) {
                // This is a TrackMate app-specific QR code
                String userId = jsonContent.getString("userId");
                String action = jsonContent.getString("action");
                
                switch (action) {
                    case "view_profile":
                        openUserProfile(userId);
                        break;
                    case "add_friend":
                        addFriend(userId);
                        break;
                    case "view_items":
                        viewUserItems(userId);
                        break;
                    default:
                        openUserProfile(userId);
                        break;
                }
            } else if (jsonContent.has("userId")) {
                // Legacy QR code with just user data
                openUserProfile(jsonContent.getString("userId"));
            } else {
                // Not a recognized format, try to handle as URL
                handleAsUrl(content);
            }
        } catch (JSONException e) {
            // Not a JSON format, try to handle as URL
            handleAsUrl(content);
        }
    }

    private void openUserProfile(String userId) {
        FirebaseService.getDatabase().child("users").child(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    progressBar.setVisibility(View.GONE);
                    
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        Intent intent = new Intent(QRScannerActivity.this, UserProfileActivity.class);
                        intent.putExtra("user_id", userId);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(QRScannerActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                        resetScanner();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(QRScannerActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    resetScanner();
                }
            });
    }
    
    private void addFriend(String userId) {
        String currentUserId = FirebaseService.getCurrentUser().getUid();
        if (currentUserId.equals(userId)) {
            Toast.makeText(this, "Cannot add yourself as a friend", Toast.LENGTH_SHORT).show();
            resetScanner();
            return;
        }
        
        FirebaseService.getDatabase().child("friendships").child(currentUserId).child(userId).setValue(true)
            .addOnSuccessListener(aVoid -> {
                // Also add reverse relationship
                FirebaseService.getDatabase().child("friendships").child(userId).child(currentUserId).setValue(true)
                    .addOnSuccessListener(aVoid2 -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(QRScannerActivity.this, "Friend added successfully!", Toast.LENGTH_SHORT).show();
                        openUserProfile(userId);
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(QRScannerActivity.this, "Error adding friend", Toast.LENGTH_SHORT).show();
                        resetScanner();
                    });
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(QRScannerActivity.this, "Error adding friend", Toast.LENGTH_SHORT).show();
                resetScanner();
            });
    }
    
    private void viewUserItems(String userId) {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("user_id", userId);
        startActivity(intent);
        finish();
    }
    
    private void handleAsUrl(String content) {
        // Check if the content is a URL
        if (content.startsWith("http://") || content.startsWith("https://")) {
            // It's a URL, open it in a browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(content));
            startActivity(browserIntent);
            finish();
        } else {
            // Not a URL either, show the content as text
            Toast.makeText(this, "Scanned content: " + content, Toast.LENGTH_LONG).show();
            resetScanner();
        }
    }
    
    private void resetScanner() {
        isScanned = false;
        barcodeView.resume();
    }
}
