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

        binding.buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.inputEmail.getText() != null ? binding.inputEmail.getText().toString().trim() : "";
                String password = binding.inputPassword.getText() != null ? binding.inputPassword.getText().toString() : "";

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
                                Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                startMainAndFinish();
                            }
                        } else {
                            binding.buttonLogin.setEnabled(true);
                            binding.buttonLogin.setText(com.example.myapplication.R.string.action_signin);
                            binding.inputPassword.setError("Invalid credentials");
                            Toast.makeText(LoginActivity.this, "Login failed. Check your email or password.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        binding.buttonLogin.setEnabled(true);
                        binding.buttonLogin.setText(com.example.myapplication.R.string.action_signin);
                        Toast.makeText(LoginActivity.this, "Error connecting to Firebase: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            }
        });

        binding.tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });

        binding.tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this, "Password recovery coming soon!", Toast.LENGTH_SHORT).show();
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




