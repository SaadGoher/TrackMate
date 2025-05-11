package com.example.trackmate;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.trackmate.models.User;
import com.example.trackmate.services.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.text.Editable;
import android.text.TextWatcher;

public class EditProfileActivity extends AppCompatActivity {

    private EditText fullNameInput, emailInput, contactInput, homeInput, streetInput, cityInput, countryInput;
    private MaterialButton saveButton, changeProfilePicButton;
    private ShapeableImageView profileImage;
    private Uri profileImageUri;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        profileImage = findViewById(R.id.profile_image);
        changeProfilePicButton = findViewById(R.id.change_profile_pic_button);
        fullNameInput = findViewById(R.id.full_name_input);
        emailInput = findViewById(R.id.email_input);
        // Disable email editing
        emailInput.setEnabled(false);
        emailInput.setFocusable(false);
        emailInput.setFocusableInTouchMode(false);
        emailInput.setTextColor(getResources().getColor(android.R.color.darker_gray)); // Use a muted color to indicate it's not editable
        
        contactInput = findViewById(R.id.contact_input);
        homeInput = findViewById(R.id.home_input);
        streetInput = findViewById(R.id.street_input);
        cityInput = findViewById(R.id.city_input);
        countryInput = findViewById(R.id.country_input);
        saveButton = findViewById(R.id.save_button);

        // Setup TextWatchers to clear errors on typing
        setupTextWatchers();

        loadUserData();

        saveButton.setOnClickListener(v -> saveUserData());
        changeProfilePicButton.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                profileImageUri = uri;
                profileImage.setImageURI(uri);
            }
        });
    }

    private void loadUserData() {
        if (FirebaseService.getCurrentUser() == null) {
            // Handle the case where the user is not logged in
            return;
        }
        String userId = FirebaseService.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseService.getDatabase().child("users").child(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                User user = task.getResult().getValue(User.class);
                if (user != null) {
                    fullNameInput.setText(user.getFullName());
                    emailInput.setText(user.getEmail());
                    contactInput.setText(user.getContact());
                    homeInput.setText(user.getHome());
                    streetInput.setText(user.getStreet());
                    cityInput.setText(user.getCity());
                    countryInput.setText(user.getCountry());
                    if (user.getProfileImageUrl() != null) {
                        Glide.with(this).load(user.getProfileImageUrl()).into(profileImage);
                    }
                }
            } else {
                Toast.makeText(EditProfileActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserData() {
        String fullName = fullNameInput.getText().toString().trim();
        // Don't get email from the input field as it's disabled
        // Instead, get email from the current Firebase user
        String email = FirebaseService.getCurrentUser().getEmail();
        String contact = contactInput.getText().toString().trim();
        String home = homeInput.getText().toString().trim();
        String street = streetInput.getText().toString().trim();
        String city = cityInput.getText().toString().trim();
        String country = countryInput.getText().toString().trim();

        // Validate all fields (except email which is not editable)
        if (!validateInputs(fullName, email, contact, home, street, city, country)) {
            return;
        }

        String userId = FirebaseService.getCurrentUser().getUid();
        User user = new User(fullName, email, contact, home, street, city, country);

        if (profileImageUri != null) {
            FirebaseService.uploadImage(profileImageUri, task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    user.setProfileImageUrl(downloadUri.toString());
                    saveUserToDatabase(userId, user);
                } else {
                    Toast.makeText(EditProfileActivity.this, "Failed to upload profile image", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            saveUserToDatabase(userId, user);
        }
    }

    private void saveUserToDatabase(String userId, User user) {
        FirebaseService.getDatabase().child("users").child(userId).setValue(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();  // Close the activity and go back
            } else {
                Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Validates all user input fields
     * @return true if all inputs are valid, false otherwise
     */
    private boolean validateInputs(String fullName, String email, String contact, String home,
                                  String street, String city, String country) {
        boolean isValid = true;

        // Name validation
        if (fullName.isEmpty()) {
            fullNameInput.setError("Full Name is required");
            isValid = false;
        } else if (fullName.length() < 3) {
            fullNameInput.setError("Name must be at least 3 characters");
            isValid = false;
        } else {
            fullNameInput.setError(null);
        }

        // Email validation is not needed as it's not editable
        // The email field is disabled and will retain its original value

        // Contact validation
        if (contact.isEmpty()) {
            contactInput.setError("Contact is required");
            isValid = false;
        } else if (!isValidPhoneNumber(contact)) {
            contactInput.setError("Please enter a valid phone number");
            isValid = false;
        } else {
            contactInput.setError(null);
        }

        // Address validation
        if (home.isEmpty()) {
            homeInput.setError("Home Address is required");
            isValid = false;
        } else {
            homeInput.setError(null);
        }

        if (street.isEmpty()) {
            streetInput.setError("Street is required");
            isValid = false;
        } else {
            streetInput.setError(null);
        }

        if (city.isEmpty()) {
            cityInput.setError("City is required");
            isValid = false;
        } else {
            cityInput.setError(null);
        }

        if (country.isEmpty()) {
            countryInput.setError("Country is required");
            isValid = false;
        } else {
            countryInput.setError(null);
        }

        return isValid;
    }

    /**
     * Validates if a string is a valid phone number
     * @param phoneNumber the phone number to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        // Remove non-digit characters for validation
        String digitsOnly = phoneNumber.replaceAll("\\D", "");
        // Validate phone number (most phone numbers are between 7 and 15 digits)
        return digitsOnly.length() >= 7 && digitsOnly.length() <= 15;
    }

    /**
     * Sets up TextWatchers for all EditText fields to clear errors when user types
     */
    private void setupTextWatchers() {
        // Create a simple TextWatcher that clears errors
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear error when user types
                if (getCurrentFocus() instanceof EditText) {
                    ((EditText) getCurrentFocus()).setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        };

        // Apply the watcher to all editable EditText fields
        fullNameInput.addTextChangedListener(textWatcher);
        // Email input is disabled, so no TextWatcher needed
        contactInput.addTextChangedListener(textWatcher);
        homeInput.addTextChangedListener(textWatcher);
        streetInput.addTextChangedListener(textWatcher);
        cityInput.addTextChangedListener(textWatcher);
        countryInput.addTextChangedListener(textWatcher);
    }
}