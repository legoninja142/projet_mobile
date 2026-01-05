package com.example.myapplication.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.controller.LoginController;
import com.example.myapplication.controller.SessionManager;
import com.example.myapplication.databinding.ActivityLoginBinding;
import com.example.myapplication.model.User;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginController loginController;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loginController = new LoginController();
        sessionManager = new SessionManager(this);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Login button click handler
        binding.buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.tilEmail.getEditText() != null ? 
                    binding.tilEmail.getEditText().getText().toString().trim() : "";
                String password = binding.tilPassword.getEditText() != null ? 
                    binding.tilPassword.getEditText().getText().toString() : "";

                // Validate inputs
                if (email.isEmpty()) {
                    binding.tilEmail.setError("Email is required");
                    return;
                }

                if (password.isEmpty()) {
                    binding.tilPassword.setError("Password is required");
                    return;
                }

                // Clear errors
                binding.tilEmail.setError(null);
                binding.tilPassword.setError(null);

                binding.buttonLogin.setEnabled(false);
                binding.buttonLogin.setText("Signing In...");

                com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
                
                // NOT SECURE: In production, use Firebase Authentication. This is for demo purposes only.
                db.collection("users")
                    .whereEqualTo("email", email)
                    .whereEqualTo("password", password)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // User found
                            User foundUser = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                            if (foundUser != null) {
                                sessionManager.saveSession(foundUser);
                                Toast.makeText(LoginActivity.this, "Login Successful! Welcome back!", Toast.LENGTH_SHORT).show();
                                startMainAndFinish();
                            }
                        } else {
                            binding.buttonLogin.setEnabled(true);
                            binding.buttonLogin.setText("SIGN IN");
                            binding.tilPassword.setError("Invalid credentials");
                            Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        binding.buttonLogin.setEnabled(true);
                        binding.buttonLogin.setText("SIGN IN");
                        Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            }
        });

        // Sign up link click handler
        binding.tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });

        // Forgot password click handler
        binding.tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this, "Password recovery feature coming soon!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startMainAndFinish() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
