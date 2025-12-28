package com.example.myapplication.ui.course;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.controller.SessionManager;
import com.example.myapplication.model.Course;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CourseListFragment extends Fragment {

    private RecyclerView recyclerView;
    private CourseAdapter courseAdapter;
    private List<Course> courseList;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private FloatingActionButton fabAddCourse;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_course_list, container, false);

        // Initialize Firestore and SessionManager
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());

        // Initialize views
        recyclerView = root.findViewById(R.id.recycler_courses);
        fabAddCourse = root.findViewById(R.id.fab_add_course);

        // Setup RecyclerView
        courseList = new ArrayList<>();
        courseAdapter = new CourseAdapter(courseList, requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(courseAdapter);

        // Load courses from Firebase
        loadCourses();

        // Setup FAB click listener
        fabAddCourse.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AddCourseActivity.class);
            startActivity(intent);
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCourses(); // Reload courses when returning to this fragment
    }

    private void loadCourses() {
        String userEmail = sessionManager.getLoggedInEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("courses")
                .whereEqualTo("userId", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    courseList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Course course = document.toObject(Course.class);
                        courseList.add(course);
                    }
                    courseAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error loading courses: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
