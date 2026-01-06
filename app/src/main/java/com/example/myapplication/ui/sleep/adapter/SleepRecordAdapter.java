package com.example.myapplication.ui.sleep.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.SleepRecord;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SleepRecordAdapter extends RecyclerView.Adapter<SleepRecordAdapter.ViewHolder> {

    private List<SleepRecord> sleepRecords;
    private final OnSleepRecordClickListener clickListener;
    private final OnSleepRecordDeleteListener deleteListener;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    public interface OnSleepRecordClickListener {
        void onSleepRecordClick(SleepRecord record);
    }

    public interface OnSleepRecordDeleteListener {
        void onSleepRecordDelete(SleepRecord record);
    }

    public SleepRecordAdapter(OnSleepRecordClickListener clickListener,
                              OnSleepRecordDeleteListener deleteListener) {
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sleep_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SleepRecord record = sleepRecords.get(position);

        if (record.getSleepStartTime() != null && record.getSleepEndTime() != null) {
            String startTime = dateTimeFormat.format(record.getSleepStartTime());
            String endTime = dateTimeFormat.format(record.getSleepEndTime());
            holder.textSleepTime.setText(String.format("%s - %s", startTime, endTime));
        }

        int hours = record.getSleepDuration() / 60;
        int minutes = record.getSleepDuration() % 60;
        holder.textDuration.setText(String.format("%dh %dm", hours, minutes));

        holder.textQuality.setText(getQualityText(record.getSleepQuality()));

        // Add color coding for quality
        holder.textQuality.setBackgroundResource(getQualityBackground(record.getSleepQuality()));

        if (record.getNotes() != null && !record.getNotes().isEmpty()) {
            holder.textNotes.setVisibility(View.VISIBLE);
            holder.textNotes.setText(record.getNotes());
        } else {
            holder.textNotes.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onSleepRecordClick(record));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onSleepRecordDelete(record));
    }

    private String getQualityText(int quality) {
        switch (quality) {
            case 1: return "Very Poor";
            case 2: return "Poor";
            case 3: return "Average";
            case 4: return "Good";
            case 5: return "Excellent";
            default: return "Unknown";
        }
    }

    private int getQualityBackground(int quality) {
        switch (quality) {
            case 1: return R.drawable.quality_bg_red;
            case 2: return R.drawable.quality_bg_orange;
            case 3: return R.drawable.quality_bg_yellow;
            case 4: return R.drawable.quality_bg_green;
            case 5: return R.drawable.quality_bg_blue;
            default: return R.drawable.quality_bg;
        }
    }

    @Override
    public int getItemCount() {
        return sleepRecords != null ? sleepRecords.size() : 0;
    }

    public void submitList(List<SleepRecord> records) {
        this.sleepRecords = records;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textSleepTime;
        TextView textDuration;
        TextView textQuality;
        TextView textDate;
        TextView textNotes;
        View btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            textSleepTime = itemView.findViewById(R.id.text_sleep_time);
            textDuration = itemView.findViewById(R.id.text_duration);
            textQuality = itemView.findViewById(R.id.text_quality);
            textDate = itemView.findViewById(R.id.text_date);
            textNotes = itemView.findViewById(R.id.text_notes);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}