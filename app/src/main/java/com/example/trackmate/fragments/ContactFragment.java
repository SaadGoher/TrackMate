package com.example.trackmate.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.trackmate.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ContactFragment extends Fragment {
    
    // UI Elements
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText subjectInput;
    private TextInputEditText messageInput;
    private MaterialButton submitButton;
    private LinearLayout emailContact;
    private LinearLayout phoneContact;
    private ImageView facebookIcon;
    private ImageView twitterIcon;
    private ImageView instagramIcon;
    private ImageView youtubeIcon;
    
    // Firebase
    private DatabaseReference mDatabase;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);
        
        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        initializeViews(view);
        setupListeners();
        
        return view;
    }
    
    private void initializeViews(View view) {
        // Contact form
        nameInput = view.findViewById(R.id.name_input);
        emailInput = view.findViewById(R.id.email_input);
        subjectInput = view.findViewById(R.id.subject_input);
        messageInput = view.findViewById(R.id.message_input);
        submitButton = view.findViewById(R.id.submit_button);
        
        // Contact info
        emailContact = view.findViewById(R.id.email_contact);
        phoneContact = view.findViewById(R.id.phone_contact);
        
        // Social media icons
        facebookIcon = view.findViewById(R.id.facebook_icon);
        twitterIcon = view.findViewById(R.id.twitter_icon);
        instagramIcon = view.findViewById(R.id.instagram_icon);
        youtubeIcon = view.findViewById(R.id.youtube_icon);
    }
    
    private void setupListeners() {
        // Submit button
        submitButton.setOnClickListener(v -> {
            if (validateForm()) {
                sendContactForm();
            }
        });
        
        // Email contact
        emailContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@trackmate.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Customer Support Request");
            
            if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Phone contact
        phoneContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+923365807090"));
            
            if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "No phone app found", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Social media icons
        facebookIcon.setOnClickListener(v -> openSocialMedia("https://www.facebook.com/trackmate"));
        twitterIcon.setOnClickListener(v -> openSocialMedia("https://www.twitter.com/trackmate"));
        instagramIcon.setOnClickListener(v -> openSocialMedia("https://www.instagram.com/trackmate"));
        youtubeIcon.setOnClickListener(v -> openSocialMedia("https://www.youtube.com/trackmate"));
    }
    
    private boolean validateForm() {
        boolean valid = true;
        
        // Validate name
        String name = nameInput.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Name is required");
            valid = false;
        } else {
            nameInput.setError(null);
        }
        
        // Validate email
        String email = emailInput.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            valid = false;
        } else {
            emailInput.setError(null);
        }
        
        // Validate subject
        String subject = subjectInput.getText().toString().trim();
        if (TextUtils.isEmpty(subject)) {
            subjectInput.setError("Subject is required");
            valid = false;
        } else {
            subjectInput.setError(null);
        }
        
        // Validate message
        String message = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            messageInput.setError("Message is required");
            valid = false;
        } else {
            messageInput.setError(null);
        }
        
        return valid;
    }
    
    private void sendContactForm() {
        // Get form data
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String subject = subjectInput.getText().toString().trim();
        String message = messageInput.getText().toString().trim();
        
        // Create a unique key for the new contact message
        String contactId = mDatabase.child("contact_messages").push().getKey();
        
        // Create a contact message object
        Map<String, Object> contactValues = new HashMap<>();
        contactValues.put("name", name);
        contactValues.put("email", email);
        contactValues.put("subject", subject);
        contactValues.put("message", message);
        contactValues.put("timestamp", System.currentTimeMillis());
        contactValues.put("status", "unread"); // For admin tracking
        
        // Create database updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("/contact_messages/" + contactId, contactValues);
        
        // Submit to Firebase
        mDatabase.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                // Show success message
                Toast.makeText(requireContext(), "Message sent successfully!", Toast.LENGTH_LONG).show();
                
                // Clear form fields
                nameInput.setText("");
                emailInput.setText("");
                subjectInput.setText("");
                messageInput.setText("");
            })
            .addOnFailureListener(e -> {
                // Show error message
                Toast.makeText(requireContext(), "Failed to send message: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
    
    private void openSocialMedia(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), "No web browser found", Toast.LENGTH_SHORT).show();
        }
    }
}