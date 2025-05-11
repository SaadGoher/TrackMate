package com.example.trackmate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.example.trackmate.services.FirebaseService;
import com.example.trackmate.utils.SharedPrefsUtil;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class SignInActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        progressBar = findViewById(R.id.progress_bar);

        MaterialButton loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(v -> loginUser());

        TextView signUpText = findViewById(R.id.sign_up_text);
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        TextView forgotPassword = findViewById(R.id.forgot_password);
        forgotPassword.setOnClickListener(v -> {
            showPasswordResetDialog();
        });
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        FirebaseService.getAuth().signOut();
        SharedPrefsUtil.clearUserData(this);
        
        // Now attempt to log in
        FirebaseService.signIn(email, password, task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                if (FirebaseService.getCurrentUser() != null) {
                    String userId = FirebaseService.getCurrentUser().getUid();
                    SharedPrefsUtil.setLoggedIn(SignInActivity.this, true);
                    SharedPrefsUtil.setUserId(SignInActivity.this, userId);
                    
                    Log.d("SignInActivity", "Login successful for user: " + userId);
                    
                    Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            } else {
                String errorMessage = "Authentication failed.";
                if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                    errorMessage = "No account found with this email.";
                } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                    errorMessage = "Invalid password.";
                }
                Toast.makeText(SignInActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                Log.e("SignInActivity", "Login error: " + task.getException().getMessage());
            }
        });
    }

    private void showPasswordResetDialog() {
        // Create dialog with an EditText for email input
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Enter your email address to receive a password reset link");

        // Set up the input
        final EditText input = new EditText(this);
        // If user already typed email in login form, pre-fill it
        String prefillEmail = emailInput.getText().toString().trim();
        if (!TextUtils.isEmpty(prefillEmail)) {
            input.setText(prefillEmail);
        }

        input.setHint("Email Address");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(SignInActivity.this, "Please enter your email address", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress
            progressBar.setVisibility(View.VISIBLE);

            // Send password reset email
            FirebaseService.resetPassword(email, task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    Toast.makeText(SignInActivity.this,
                            "Password reset email sent to " + email,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SignInActivity.this,
                            "Failed to send reset email: " + task.getException().getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}