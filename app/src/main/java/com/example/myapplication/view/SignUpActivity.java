package com.example.myapplication.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.controller.SessionManager;
import com.example.myapplication.databinding.ActivitySignupBinding;
import com.example.myapplication.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        binding.buttonSignUp.setOnClickListener(v -> signUp());
        binding.tvSignIn.setOnClickListener(v -> finish());
    }

    private void signUp() {

        String fullName = binding.inputFullName.getText().toString().trim();
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString();
        String confirm = binding.inputConfirmPassword.getText().toString();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Champs requis ❌", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            binding.inputConfirmPassword.setError("Mots de passe différents");
            return;
        }

        binding.buttonSignUp.setEnabled(false);
        binding.buttonSignUp.setText("Création...");

        // 🔑 Création du compte dans Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) return;

                    // 🔑 Création du document utilisateur dans Firestore avec UID Firebase
                    User user = new User(fullName, email,password);
                    db.collection("users")
                            .document(firebaseUser.getUid()) // UID Auth
                            .set(user)
                            .addOnSuccessListener(unused -> {

                                // 🔑 Sauvegarde session
                                sessionManager.saveSession(user);

                                // 🔑 Aller vers MainActivity
                                Intent intent = new Intent(this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Erreur Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                binding.buttonSignUp.setEnabled(true);
                                binding.buttonSignUp.setText(R.string.action_signup);
                            });

                })
                .addOnFailureListener(e -> {
                    binding.buttonSignUp.setEnabled(true);
                    binding.buttonSignUp.setText(R.string.action_signup);
                    Toast.makeText(this, "Erreur Auth: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
