package com.example.trackmate;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.trackmate.models.User;
import com.example.trackmate.services.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditProfileActivity extends AppCompatActivity {

    private EditText fullNameInput, emailInput, contactInput, homeInput, streetInput, cityInput, countryInput;
    private MaterialButton saveButton;

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

        fullNameInput = findViewById(R.id.full_name_input);
        emailInput = findViewById(R.id.email_input);
        contactInput = findViewById(R.id.contact_input);
        homeInput = findViewById(R.id.home_input);
        streetInput = findViewById(R.id.street_input);
        cityInput = findViewById(R.id.city_input);
        countryInput = findViewById(R.id.country_input);
        saveButton = findViewById(R.id.save_button);

        loadUserData();

        saveButton.setOnClickListener(v -> saveUserData());
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
                }
            } else {
                Toast.makeText(EditProfileActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserData() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String contact = contactInput.getText().toString().trim();
        String home = homeInput.getText().toString().trim();
        String street = streetInput.getText().toString().trim();
        String city = cityInput.getText().toString().trim();
        String country = countryInput.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || contact.isEmpty() || home.isEmpty() || street.isEmpty() || city.isEmpty() || country.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseService.getCurrentUser().getUid();
        User user = new User(fullName, email, contact, home, street, city, country);
        FirebaseService.getDatabase().child("users").child(userId).setValue(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}