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
            // Handle forgot password logic
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

        FirebaseService.signIn(email, password, task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                if (FirebaseService.getCurrentUser() != null) {
                    SharedPrefsUtil.setLoggedIn(SignInActivity.this, true);
                    SharedPrefsUtil.setUserId(SignInActivity.this, FirebaseService.getCurrentUser().getUid());
                }
                Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                String errorMessage = "Authentication failed.";
                if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                    errorMessage = "No account found with this email.";
                } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                    errorMessage = "Invalid password.";
                }
                Toast.makeText(SignInActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}