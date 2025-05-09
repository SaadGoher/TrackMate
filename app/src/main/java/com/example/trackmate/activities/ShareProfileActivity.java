package com.example.trackmate.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.trackmate.R;
import com.example.trackmate.models.User;
import com.example.trackmate.services.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;


import org.json.JSONObject;

import java.io.OutputStream;

public class ShareProfileActivity extends AppCompatActivity {
    private ImageView qrCodeImage;
    private Bitmap qrBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_profile);

        setupToolbar();
        
        qrCodeImage = findViewById(R.id.qr_code_image);
        MaterialButton shareButton = findViewById(R.id.share_button);
        MaterialButton saveButton = findViewById(R.id.save_button);

        loadUserDataAndGenerateQR();

        shareButton.setOnClickListener(v -> shareQRCode());
        saveButton.setOnClickListener(v -> saveQRCode());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Share Profile");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadUserDataAndGenerateQR() {
        String userId = FirebaseService.getCurrentUser().getUid();
        FirebaseService.getDatabase().child("users").child(userId).get()
            .addOnSuccessListener(dataSnapshot -> {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    generateQRCode(user);
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show());
    }

    private void generateQRCode(User user) {
        try {
            JSONObject userData = new JSONObject();
            String userId = FirebaseService.getCurrentUser().getUid();
            
            // Add basic user identification
            userData.put("userId", userId);
            userData.put("appId", "com.example.trackmate"); // App identifier
            userData.put("action", "view_profile"); // Default action
            
            // Add detailed user information
            userData.put("name", user.getFullName());
            userData.put("email", user.getEmail());
            userData.put("contact", user.getContact());
            
            // Add address information if available
            if (user.getHome() != null && !user.getHome().isEmpty()) {
                userData.put("home", user.getHome());
            }
            if (user.getStreet() != null && !user.getStreet().isEmpty()) {
                userData.put("street", user.getStreet());
            }
            if (user.getCity() != null && !user.getCity().isEmpty()) {
                userData.put("city", user.getCity());
            }
            if (user.getCountry() != null && !user.getCountry().isEmpty()) {
                userData.put("country", user.getCountry());
            }
            
            // Add timestamp for verification
            userData.put("timestamp", System.currentTimeMillis());

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(userData.toString(), BarcodeFormat.QR_CODE, 512, 512);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            qrBitmap = barcodeEncoder.createBitmap(bitMatrix);
            qrCodeImage.setImageBitmap(qrBitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQRCode() {
        if (qrBitmap == null) return;

        try {
            String fileName = "trackmate_profile_" + System.currentTimeMillis() + ".png";
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
                startActivity(Intent.createChooser(shareIntent, "Share Profile QR Code"));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveQRCode() {
        if (qrBitmap == null) return;

        try {
            String fileName = "trackmate_profile_" + System.currentTimeMillis() + ".png";
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
            Toast.makeText(this, "Error saving QR code", Toast.LENGTH_SHORT).show();
        }
    }
}