package com.example.myapplication.ui.run;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Course;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private final List<Course> courses;

    public CourseAdapter(List<Course> courses) {
        this.courses = courses;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course c = courses.get(position);

        // Temps de début
        holder.tvStartTime.setText("Start: " + Course.formatTimestamp(c.getStartTime()));

        // Durée en HH:MM:SS
        long durationSec = c.getDuration() / 1000;
        long hrs = durationSec / 3600;
        long mins = (durationSec % 3600) / 60;
        long secs = durationSec % 60;
        holder.tvDuration.setText(String.format("Duration: %02d:%02d:%02d", hrs, mins, secs));

        // Distance en mètres
        holder.tvDistance.setText("Distance: " + (int)c.getDistance() + " m");

        // Vitesse moyenne en m/s
        holder.tvAvgSpeed.setText(String.format("Avg Speed: %.2f m/s", c.getAvgSpeed()));
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView tvStartTime, tvDuration, tvDistance, tvAvgSpeed;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStartTime = itemView.findViewById(R.id.tvStartTime);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvAvgSpeed = itemView.findViewById(R.id.tvAvgSpeed);
        }
    }
}
