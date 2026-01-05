package com.example.myapplication.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
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
                String fullName = binding.tilFullName.getEditText() != null ? 
                    binding.tilFullName.getEditText().getText().toString().trim() : "";
                String email = binding.tilEmail.getEditText() != null ? 
                    binding.tilEmail.getEditText().getText().toString().trim() : "";
                String phone = binding.tilPhone.getEditText() != null ? 
                    binding.tilPhone.getEditText().getText().toString().trim() : "";
                String age = binding.tilAge.getEditText() != null ? 
                    binding.tilAge.getEditText().getText().toString().trim() : "";
                String password = binding.tilPassword.getEditText() != null ? 
                    binding.tilPassword.getEditText().getText().toString() : "";
                String confirmPassword = binding.tilConfirmPassword.getEditText() != null ? 
                    binding.tilConfirmPassword.getEditText().getText().toString() : "";

                // Get gender
                String gender = "";
                int selectedGenderId = binding.rgGender.getCheckedRadioButtonId();
                if (selectedGenderId != -1) {
                    RadioButton selectedGender = findViewById(selectedGenderId);
                    gender = selectedGender.getText().toString();
                }

                // Validate inputs
                boolean isValid = true;

                if (fullName.isEmpty()) {
                    binding.tilFullName.setError("Full name is required");
                    isValid = false;
                } else {
                    binding.tilFullName.setError(null);
                }

                if (email.isEmpty()) {
                    binding.tilEmail.setError("Email is required");
                    isValid = false;
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    binding.tilEmail.setError("Invalid email format");
                    isValid = false;
                } else {
                    binding.tilEmail.setError(null);
                }

                if (phone.isEmpty()) {
                    binding.tilPhone.setError("Phone number is required");
                    isValid = false;
                } else {
                    binding.tilPhone.setError(null);
                }

                if (password.isEmpty()) {
                    binding.tilPassword.setError("Password is required");
                    isValid = false;
                } else if (password.length() < 6) {
                    binding.tilPassword.setError("Password must be at least 6 characters");
                    isValid = false;
                } else {
                    binding.tilPassword.setError(null);
                }

                if (!password.equals(confirmPassword)) {
                    binding.tilConfirmPassword.setError("Passwords do not match");
                    isValid = false;
                } else {
                    binding.tilConfirmPassword.setError(null);
                }

                if (!binding.cbTerms.isChecked()) {
                    Toast.makeText(SignUpActivity.this, "Please accept Terms & Conditions", Toast.LENGTH_SHORT).show();
                    isValid = false;
                }

                if (!isValid) {
                    return;
                }

                // Create user object
                User user = new User(fullName, email, password);

                // Save to Firebase Firestore
                com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
                
                binding.buttonSignUp.setEnabled(false);
                binding.buttonSignUp.setText("Creating Account...");
                
                db.collection("users")
                    .add(user)
                    .addOnSuccessListener(documentReference -> {
                        // Save local session
                        sessionManager.saveSession(user);
                        
                        Toast.makeText(SignUpActivity.this, "Account created successfully! Welcome!", Toast.LENGTH_SHORT).show();
                        
                        // Go to MainActivity
                        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        binding.buttonSignUp.setEnabled(true);
                        binding.buttonSignUp.setText("CREATE ACCOUNT");
                        Toast.makeText(SignUpActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
