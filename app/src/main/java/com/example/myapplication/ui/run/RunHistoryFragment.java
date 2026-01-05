package com.example.myapplication.ui.run;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.controller.SessionManager;
import com.example.myapplication.model.Course;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RunHistoryFragment extends Fragment {

    private RecyclerView rvCourses;
    private CourseAdapter adapter;
    private final List<Course> courses = new ArrayList<>();
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_run_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCourses = view.findViewById(R.id.rvCourses);
        rvCourses.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CourseAdapter(courses);
        rvCourses.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireActivity());

        loadCourses();
    }

    private void loadCourses() {
        String email = sessionManager.getLoggedInEmail();
        if (email == null || email.isEmpty()) return;

        String safeUserId = email.replace(".", "_");

        db.collection("users")
                .document(safeUserId)
                .collection("courses")
                .orderBy("startTime")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    courses.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Course c = doc.toObject(Course.class);
                        courses.add(c);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // Si erreur Firestore
                    e.printStackTrace();
                });
    }
}
