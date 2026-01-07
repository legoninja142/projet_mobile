package com.example.myapplication.ui.sleep;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.model.SleepRecord;
import com.example.myapplication.repository.SleepRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SleepViewModel extends ViewModel {
    private final MutableLiveData<List<SleepRecord>> sleepRecords = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final SleepRepository repository = new SleepRepository();
    private final MutableLiveData<SleepRecord> selectedRecord = new MutableLiveData<>();

    public LiveData<List<SleepRecord>> getSleepRecords() {
        return sleepRecords;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<SleepRecord> getSelectedRecord() {
        return selectedRecord;
    }

    public void loadSleepRecords(String userId) {
        isLoading.setValue(true);
        repository.getSleepRecordsByUser(userId, task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                List<SleepRecord> records = new ArrayList<>();
                for (DocumentSnapshot document : task.getResult()) {
                    SleepRecord record = document.toObject(SleepRecord.class);
                    if (record != null) {
                        record.setId(document.getId());
                        records.add(record);
                    }
                }

                // Sort records by recordDate in descending order (newest first)
                // This handles both normal queries and fallback queries
                records.sort((r1, r2) -> {
                    if (r1.getRecordDate() == null || r2.getRecordDate() == null) {
                        return 0;
                    }
                    return r2.getRecordDate().compareTo(r1.getRecordDate());
                });

                sleepRecords.setValue(records);
            } else {
                Exception exception = task.getException();
                String errorMsg = exception != null ? exception.getMessage() : "Unknown error";

                // Handle specific Firestore errors
                if (errorMsg.contains("FAILED_PRECONDITION")) {
                    errorMessage.setValue("Creating composite index... Please wait and try again in a few minutes. If error persists, create index manually in Firebase Console.");
                } else if (errorMsg.contains("PERMISSION_DENIED")) {
                    errorMessage.setValue("Permission denied. Please check Firestore security rules.");
                } else if (errorMsg.contains("UNAUTHENTICATED")) {
                    errorMessage.setValue("Not authenticated. Please log in again.");
                } else {
                    errorMessage.setValue("Failed to load sleep records: " + errorMsg);
                }
            }
        });
    }

    public void addSleepRecord(SleepRecord record, String userId) {
        isLoading.setValue(true);
        record.setUserId(userId);
        repository.addSleepRecord(record, task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                loadSleepRecords(userId);
            } else {
                Exception exception = task.getException();
                String errorMsg = exception != null ? exception.getMessage() : "Unknown error";

                // Check for specific permission/authentication errors
                if (errorMsg.contains("PERMISSION_DENIED")) {
                    errorMessage.setValue("Permission denied. Please check Firestore security rules and ensure you're logged in.");
                } else if (errorMsg.contains("UNAUTHENTICATED")) {
                    errorMessage.setValue("Not authenticated. Please log in again.");
                } else if (errorMsg.contains("NETWORK")) {
                    errorMessage.setValue("Network error. Please check your internet connection.");
                } else {
                    errorMessage.setValue("Failed to add sleep record: " + errorMsg);
                }
            }
        });
    }

    public void updateSleepRecord(String recordId, SleepRecord record) {
        isLoading.setValue(true);
        repository.updateSleepRecord(recordId, record, task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                loadSleepRecords(record.getUserId());
            } else {
                errorMessage.setValue("Failed to update sleep record: " + task.getException().getMessage());
            }
        });
    }

    public void deleteSleepRecord(String recordId, String userId) {
        isLoading.setValue(true);
        repository.deleteSleepRecord(recordId, task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                loadSleepRecords(userId);
            } else {
                errorMessage.setValue("Failed to delete sleep record: " + task.getException().getMessage());
            }
        });
    }

    public void selectRecord(SleepRecord record) {
        selectedRecord.setValue(record);
    }

    public void clearSelectedRecord() {
        selectedRecord.setValue(null);
    }
}