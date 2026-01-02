package com.example.myapplication.repository;

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
    private final FirebaseFirestore db;
    private final CollectionReference sleepCollection;

    public SleepRepository() {
        db = FirebaseFirestore.getInstance();
        sleepCollection = db.collection(COLLECTION_NAME);
    }

    public void addSleepRecord(SleepRecord record, OnCompleteListener<DocumentReference> listener) {
        sleepCollection.add(record)
                .addOnCompleteListener(listener);
    }

    public void getSleepRecordsByUser(String userId, OnCompleteListener<QuerySnapshot> listener) {
        sleepCollection.whereEqualTo("userId", userId)
                .orderBy("recordDate", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(listener);
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