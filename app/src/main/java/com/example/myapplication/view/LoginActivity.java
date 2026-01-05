package com.example.myapplication.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.controller.SessionManager;
import com.example.myapplication.databinding.ActivityLoginBinding;
import com.example.myapplication.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        binding.buttonLogin.setOnClickListener(v -> login());
        binding.tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
    }

    private void login() {

        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Champs requis ❌", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.buttonLogin.setEnabled(false);
        binding.buttonLogin.setText("Connexion...");

        // 🔑 Connexion Firebase Auth
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) return;

                    // 🔑 Récupération des infos depuis Firestore
                    db.collection("users")
                            .document(firebaseUser.getUid())
                            .get()
                            .addOnSuccessListener(snapshot -> {

                                User user = snapshot.toObject(User.class);
                                if (user != null) {
                                    sessionManager.saveSession(user);
                                }

                                // 🔑 Aller vers MainActivity
                                Intent intent = new Intent(this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });

                })
                .addOnFailureListener(e -> {
                    binding.buttonLogin.setEnabled(true);
                    binding.buttonLogin.setText(R.string.action_signin);
                    Toast.makeText(this, "Connexion échouée: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
