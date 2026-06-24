package com.example.mainproj.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GateLogAdapter extends RecyclerView.Adapter<GateLogAdapter.LogViewHolder> {

    public static class GateLogEntry {
        public String gate;
        public String ip;
        public String user;
        public long timestamp;
        public String fromTo;
        public String commandId;
        public String action;
        public String dateKey;
        public String timeKey;

        public GateLogEntry(String gate, String ip, String user, long timestamp,
                            String fromTo, String commandId, String action,
                            String dateKey, String timeKey) {
            this.gate = gate;
            this.ip = ip;
            this.user = user;
            this.timestamp = timestamp;
            this.fromTo = fromTo;
            this.commandId = commandId;
            this.action = action;
            this.dateKey = dateKey;
            this.timeKey = timeKey;
        }
    }

    private final List<GateLogEntry> entries;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.US);

    public GateLogAdapter(List<GateLogEntry> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gate_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        GateLogEntry entry = entries.get(position);
        holder.tvUser.setText(entry.user != null ? entry.user : "Unknown");
        holder.tvGate.setText(entry.gate != null ? entry.gate : "Unknown");
        holder.tvFromTo.setText(entry.fromTo != null ? entry.fromTo : "");
        holder.tvTimestamp.setText(dateFormat.format(new Date(entry.timestamp * 1000L)));
        holder.tvIp.setText(entry.ip != null ? entry.ip : "");

        boolean isOpen = "open".equalsIgnoreCase(entry.action);
        holder.tvAction.setText(isOpen ? "OPEN" : "CLOSE");
        holder.tvAction.setBackgroundResource(isOpen
                ? R.drawable.status_badge_open
                : R.drawable.status_badge_closed);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvGate, tvAction, tvFromTo, tvTimestamp, tvIp;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tvLogUser);
            tvGate = itemView.findViewById(R.id.tvLogGate);
            tvAction = itemView.findViewById(R.id.tvLogAction);
            tvFromTo = itemView.findViewById(R.id.tvLogFromTo);
            tvTimestamp = itemView.findViewById(R.id.tvLogTimestamp);
            tvIp = itemView.findViewById(R.id.tvLogIp);
        }
    }
}
