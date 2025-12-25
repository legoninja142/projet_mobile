package com.example.myapplication.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.controller.SessionManager;
import com.example.myapplication.databinding.ActivitySignupBinding;
import com.example.myapplication.model.User;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullName = binding.inputFullName.getText() != null ? binding.inputFullName.getText().toString().trim() : "";
                String email = binding.inputEmail.getText() != null ? binding.inputEmail.getText().toString().trim() : "";
                String password = binding.inputPassword.getText() != null ? binding.inputPassword.getText().toString() : "";
                String confirmPassword = binding.inputConfirmPassword.getText() != null ? binding.inputConfirmPassword.getText().toString() : "";

                if (fullName.isEmpty()) {
                    binding.inputFullName.setError("Full name is required");
                    return;
                }

                if (email.isEmpty()) {
                    binding.inputEmail.setError("Email is required");
                    return;
                }

                if (password.isEmpty()) {
                    binding.inputPassword.setError("Password is required");
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    binding.inputConfirmPassword.setError("Passwords do not match");
                    return;
                }

                // Create user object
                User user = new User(fullName, email, password);

                // Save to Firebase Firestore
                com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
                
                binding.buttonSignUp.setEnabled(false); // Prevent multiple clicks
                binding.buttonSignUp.setText("Creating Account...");
                
                db.collection("users")
                    .add(user)
                    .addOnSuccessListener(documentReference -> {
                        // Save local session
                        sessionManager.saveSession(user);
                        
                        Toast.makeText(SignUpActivity.this, "Account created & saved to Firebase!", Toast.LENGTH_SHORT).show();
                        
                        // Go to MainActivity
                        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        binding.buttonSignUp.setEnabled(true);
                        binding.buttonSignUp.setText(com.example.myapplication.R.string.action_signup);
                        Toast.makeText(SignUpActivity.this, "Error saving to Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            }
        });

        binding.tvSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to login
            }
        });
    }
}

