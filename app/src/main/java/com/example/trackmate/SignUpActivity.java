package com.example.trackmate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.trackmate.models.User;
import com.example.trackmate.services.FirebaseService;
import com.example.trackmate.utils.SharedPrefsUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {

    private EditText fullNameInput, emailInput, contactInput, homeInput, streetInput, cityInput, countryInput, passwordInput;
    private ProgressBar progressBar;
    private ShapeableImageView profileImage;
    private Uri profileImageUri;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fullNameInput = findViewById(R.id.full_name_input);
        emailInput = findViewById(R.id.email_input);
        contactInput = findViewById(R.id.contact_input);
        homeInput = findViewById(R.id.home_input);
        streetInput = findViewById(R.id.street_input);
        cityInput = findViewById(R.id.city_input);
        countryInput = findViewById(R.id.country_input);
        passwordInput = findViewById(R.id.password_input);
        progressBar = findViewById(R.id.progress_bar);
        profileImage = findViewById(R.id.profile_image);
        MaterialButton selectImageButton = findViewById(R.id.select_image_button);

        // Setup TextWatchers to clear errors on typing
        setupTextWatchers();

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                profileImageUri = uri;
                profileImage.setImageURI(uri);
            }
        });

        selectImageButton.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        MaterialButton signUpButton = findViewById(R.id.sign_up_button);
        signUpButton.setOnClickListener(v -> signUpUser());

        TextView signInText = findViewById(R.id.sign_in_text);
        signInText.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void signUpUser() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String contact = contactInput.getText().toString().trim();
        String home = homeInput.getText().toString().trim();
        String street = streetInput.getText().toString().trim();
        String city = cityInput.getText().toString().trim();
        String country = countryInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validate all fields
        if (!validateInputs(fullName, email, contact, home, street, city, country, password)) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        FirebaseService.createUser(email, password, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = FirebaseService.getCurrentUser();
                if (user != null) {
                    if (profileImageUri != null) {
                        uploadProfileImageAndSaveUser(user.getUid(), fullName, email, contact, home, street, city, country);
                    } else {
                        saveUserToDatabase(user.getUid(), fullName, email, contact, home, street, city, country, null);
                    }
                }
            } else {
                progressBar.setVisibility(View.GONE);
                String errorMessage = "Sign Up failed.";
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    errorMessage = "This email is already registered.";
                } else if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                    errorMessage = "Password is too weak.";
                }
                Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadProfileImageAndSaveUser(String userId, String fullName, String email, String contact, 
                                             String home, String street, String city, String country) {
        FirebaseService.uploadImage(profileImageUri, task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                saveUserToDatabase(userId, fullName, email, contact, home, street, city, country, downloadUri.toString());
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SignUpActivity.this, "Failed to upload profile image", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserToDatabase(String userId, String fullName, String email, String contact,
                                  String home, String street, String city, String country, String imageUrl) {
        User userDetails = new User(fullName, email, contact, home, street, city, country);
        userDetails.setProfileImageUrl(imageUrl);
        
        FirebaseService.saveUserDetails(userId, userDetails);
        
        // Ensure preferences are properly set
        SharedPrefsUtil.clearUserData(SignUpActivity.this); // Clear any existing data first
        SharedPrefsUtil.setLoggedIn(SignUpActivity.this, true);
        SharedPrefsUtil.setUserId(SignUpActivity.this, userId);
        
        progressBar.setVisibility(View.GONE);
        
        // Use flags to prevent going back to login screen with back button
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        
        Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
    }
    
    private boolean validateInputs(String fullName, String email, String contact, String home, 
                                  String street, String city, String country, String password) {
        boolean isValid = true;
        
        // Name validation
        if (TextUtils.isEmpty(fullName)) {
            fullNameInput.setError("Full Name is required");
            isValid = false;
        } else if (fullName.length() < 3) {
            fullNameInput.setError("Name must be at least 3 characters");
            isValid = false;
        } else {
            fullNameInput.setError(null);
        }
        
        // Email validation
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email address");
            isValid = false;
        } else {
            emailInput.setError(null);
        }
        
        // Contact validation
        if (TextUtils.isEmpty(contact)) {
            contactInput.setError("Contact is required");
            isValid = false;
        } else if (!isValidPhoneNumber(contact)) {
            contactInput.setError("Please enter a valid phone number");
            isValid = false;
        } else {
            contactInput.setError(null);
        }
        
        // Address validation
        if (TextUtils.isEmpty(home)) {
            homeInput.setError("Home Address is required");
            isValid = false;
        } else {
            homeInput.setError(null);
        }
        
        if (TextUtils.isEmpty(street)) {
            streetInput.setError("Street is required");
            isValid = false;
        } else {
            streetInput.setError(null);
        }
        
        if (TextUtils.isEmpty(city)) {
            cityInput.setError("City is required");
            isValid = false;
        } else {
            cityInput.setError(null);
        }
        
        if (TextUtils.isEmpty(country)) {
            countryInput.setError("Country is required");
            isValid = false;
        } else {
            countryInput.setError(null);
        }
        
        // Password validation
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            isValid = false;
        } else if (!isStrongPassword(password)) {
            passwordInput.setError("Password should contain at least one number, one uppercase and one lowercase letter");
            isValid = false;
        } else {
            passwordInput.setError(null);
        }
        
        return isValid;
    }
    
    private boolean isValidPhoneNumber(String phoneNumber) {
        // Remove non-digit characters for validation
        String digitsOnly = phoneNumber.replaceAll("\\D", "");
        // Validate phone number (most phone numbers are between 7 and 15 digits)
        return digitsOnly.length() >= 7 && digitsOnly.length() <= 15;
    }
    
    private boolean isStrongPassword(String password) {
        boolean hasUpperCase = false;
        boolean hasLowerCase = false;
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowerCase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
            
            if (hasUpperCase && hasLowerCase && hasDigit) {
                return true;
            }
        }
        
        return false;
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
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
        
        // Apply the watcher to all EditText fields
        fullNameInput.addTextChangedListener(textWatcher);
        emailInput.addTextChangedListener(textWatcher);
        contactInput.addTextChangedListener(textWatcher);
        homeInput.addTextChangedListener(textWatcher);
        streetInput.addTextChangedListener(textWatcher);
        cityInput.addTextChangedListener(textWatcher);
        countryInput.addTextChangedListener(textWatcher);
        passwordInput.addTextChangedListener(textWatcher);
    }
}