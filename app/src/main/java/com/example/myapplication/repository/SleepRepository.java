package com.example.myapplication.repository;

import android.util.Log;

import com.example.myapplication.model.SleepRecord;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class SleepRepository {
    private static final String COLLECTION_NAME = "sleep_records";
    private static final String TAG = "SleepRepository";
    private final FirebaseFirestore db;
    private final CollectionReference sleepCollection;

    public SleepRepository() {
        db = FirebaseFirestore.getInstance();
        sleepCollection = db.collection(COLLECTION_NAME);
    }

    public void addSleepRecord(SleepRecord record, OnCompleteListener<DocumentReference> listener) {
        Log.d(TAG, "Adding sleep record for user: " + record.getUserId());
        sleepCollection.add(record)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Sleep record added successfully with ID: " + task.getResult().getId());
                    } else {
                        Log.e(TAG, "Error adding sleep record", task.getException());
                    }
                    listener.onComplete(task);
                });
    }

    public void getSleepRecordsByUser(String userId, OnCompleteListener<QuerySnapshot> listener) {
        Log.d(TAG, "Fetching sleep records for user: " + userId);
        sleepCollection.whereEqualTo("userId", userId)
                .orderBy("recordDate", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Successfully fetched " + task.getResult().size() + " sleep records");
                        listener.onComplete(task);
                    } else {
                        Exception exception = task.getException();
                        String errorMsg = exception != null ? exception.getMessage() : "Unknown error";
                        Log.e(TAG, "Error fetching sleep records: " + errorMsg, exception);

                        // Check for FAILED_PRECONDITION and try fallback
                        if (errorMsg != null && errorMsg.contains("FAILED_PRECONDITION")) {
                            Log.e(TAG, "FAILED_PRECONDITION: Composite index missing. Attempting fallback query...");
                            Log.e(TAG, "To fix permanently: Create composite index in Firebase Console:");
                            Log.e(TAG, "  Collection: sleep_records");
                            Log.e(TAG, "  Field 1: userId (Ascending)");
                            Log.e(TAG, "  Field 2: recordDate (Descending)");

                            // Fallback: Query without orderBy, sort in app
                            fetchAndSortFallback(userId, listener);
                        } else {
                            listener.onComplete(task);
                        }
                    }
                });
    }

    private void fetchAndSortFallback(String userId, OnCompleteListener<QuerySnapshot> listener) {
        Log.d(TAG, "Using fallback: Fetching unsorted, will sort in app");
        sleepCollection.whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Fallback successful: fetched " + task.getResult().size() + " records");
                    } else {
                        Log.e(TAG, "Fallback also failed: " + task.getException());
                    }
                    listener.onComplete(task);
                });
    }

    public void updateSleepRecord(String recordId, SleepRecord record, OnCompleteListener<Void> listener) {
        sleepCollection.document(recordId)
                .set(record)
                .addOnCompleteListener(listener);
    }

    public void deleteSleepRecord(String recordId, OnCompleteListener<Void> listener) {
        sleepCollection.document(recordId)
                .delete()
                .addOnCompleteListener(listener);
    }

    public void getSleepRecord(String recordId, OnCompleteListener<DocumentSnapshot> listener) {
        sleepCollection.document(recordId)
                .get()
                .addOnCompleteListener(listener);
    }
}