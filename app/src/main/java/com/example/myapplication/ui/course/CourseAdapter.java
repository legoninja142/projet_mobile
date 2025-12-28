package com.example.myapplication.ui.course;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Course;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private List<Course> courseList;
    private Context context;
    private SimpleDateFormat dateFormat;

    public CourseAdapter(List<Course> courseList, Context context) {
        this.courseList = courseList;
        this.context = context;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);

        // Format date and time
        String dateTime = dateFormat.format(new Date(course.getStartTime()));
        holder.tvDateTime.setText(dateTime);

        // Set course details
        holder.tvDistance.setText(course.getFormattedDistance());
        holder.tvDuration.setText(course.getFormattedDuration());
        holder.tvSteps.setText(String.valueOf(course.getSteps()) + " steps");
        holder.tvCalories.setText(String.valueOf(course.getCalories()) + " cal");
        holder.tvAvgSpeed.setText(course.getFormattedAvgSpeed());
        holder.tvMaxSpeed.setText(String.format("%.2f km/h", course.getMaxSpeed()));
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateTime, tvDistance, tvDuration, tvSteps, tvCalories, tvAvgSpeed, tvMaxSpeed;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tv_date_time);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvSteps = itemView.findViewById(R.id.tv_steps);
            tvCalories = itemView.findViewById(R.id.tv_calories);
            tvAvgSpeed = itemView.findViewById(R.id.tv_avg_speed);
            tvMaxSpeed = itemView.findViewById(R.id.tv_max_speed);
        }
    }
}
