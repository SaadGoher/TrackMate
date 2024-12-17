package com.example.trackmate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.trackmate.models.User;
import com.example.trackmate.services.FirebaseService;
import com.example.trackmate.utils.SharedPrefsUtil;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {

    private EditText fullNameInput, emailInput, contactInput, homeInput, streetInput, cityInput, countryInput, passwordInput;
    private ProgressBar progressBar;

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

        if (TextUtils.isEmpty(fullName)) {
            fullNameInput.setError("Full Name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(contact)) {
            contactInput.setError("Contact is required");
            return;
        }

        if (TextUtils.isEmpty(home)) {
            homeInput.setError("Home Address is required");
            return;
        }

        if (TextUtils.isEmpty(street)) {
            streetInput.setError("Street is required");
            return;
        }

        if (TextUtils.isEmpty(city)) {
            cityInput.setError("City is required");
            return;
        }

        if (TextUtils.isEmpty(country)) {
            countryInput.setError("Country is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        FirebaseService.createUser(email, password, task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                FirebaseUser user = FirebaseService.getCurrentUser();
                if (user != null) {
                    SharedPrefsUtil.setLoggedIn(SignUpActivity.this, true);
                    SharedPrefsUtil.setUserId(SignUpActivity.this, user.getUid());

                    User userDetails = new User(fullName, email, contact, home, street, city, country);
                    FirebaseService.saveUserDetails(user.getUid(), userDetails);

                    Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            } else {
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
}