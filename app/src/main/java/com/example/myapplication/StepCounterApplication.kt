package com.example.myapplication

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class StepCounterApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            // Initialiser Firebase
            FirebaseApp.initializeApp(this)

            // Configurer Firestore pour le mode hors ligne et la persistance
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings

            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously()
                    .addOnSuccessListener {
                        Log.d("StepCounterApplication", "Anonymous auth success")
                    }
                    .addOnFailureListener { e ->
                        Log.e("StepCounterApplication", "Anonymous auth failed", e)
                    }
            }
        } catch (e: Exception) {
            Log.e("StepCounterApplication", "Error initializing Firebase", e)
        }
    }
}
