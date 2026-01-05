package com.example.myapplication.ui.run;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.model.Course;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RunViewModel extends ViewModel {

    private final MutableLiveData<Boolean> running = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Course course;
    private Double lastLatitude = null;
    private Double lastLongitude = null;

    private final MutableLiveData<Float> distanceLive = new MutableLiveData<>(0f);
    private final MutableLiveData<Float> avgSpeedLive = new MutableLiveData<>(0f);
    private final MutableLiveData<Long> durationLive = new MutableLiveData<>(0L);

    public LiveData<Boolean> isRunning() { return running; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<Float> getDistanceLive() { return distanceLive; }
    public LiveData<Float> getAvgSpeedLive() { return avgSpeedLive; }
    public LiveData<Long> getDurationLive() { return durationLive; }

    // ================= START COURSE =================
    public void startRun(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            message.setValue("Utilisateur non connecté ❌");
            return;
        }

        course = new Course(userEmail, System.currentTimeMillis());
        lastLatitude = null;
        lastLongitude = null;

        running.setValue(true);
        message.setValue("Course démarrée 🏃‍♀️");
    }

    // ================= GPS UPDATE =================
    public void onLocationUpdate(double lat, double lon, float speed) {
        if (course == null) return;

        if (lastLatitude != null && lastLongitude != null) {
            float[] result = new float[1];
            android.location.Location.distanceBetween(
                    lastLatitude, lastLongitude,
                    lat, lon,
                    result
            );
            course.addDistance(result[0]); // mètres
        }

        course.updateSpeed(speed); // max speed update

        lastLatitude = lat;
        lastLongitude = lon;

        // Mettre à jour LiveData pour l'UI
        distanceLive.setValue(course.getDistance());             // m
        avgSpeedLive.setValue(course.getAvgSpeed());            // m/s
        durationLive.setValue(System.currentTimeMillis() - course.getStartTime());
    }

    // ================= STOP COURSE =================
    public void stopRun() {
        if (!Boolean.TRUE.equals(running.getValue()) || course == null) return;

        running.setValue(false);
        course.finish(System.currentTimeMillis());

        // Mettre à jour LiveData pour UI final
        distanceLive.setValue(course.getDistance());
        avgSpeedLive.setValue(course.getAvgSpeed());
        durationLive.setValue(course.getDuration());

        saveCourseToFirestore(course);
    }

    // ================= FIRESTORE =================
    private void saveCourseToFirestore(Course course) {

        String safeUserId = course.getUserId().replace(".", "_");

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(safeUserId)
                .collection("courses")
                .add(course) // ✅ on envoie l'objet Course DIRECTEMENT
                .addOnSuccessListener(doc -> {
                    doc.update("id", doc.getId());
                })
                .addOnFailureListener(e -> {
                    message.setValue("Erreur Firestore ❌ " + e.getMessage());
                });
    }


}
