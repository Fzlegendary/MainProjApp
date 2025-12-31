package com.example.mainproj;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GateAdapter extends RecyclerView.Adapter<GateAdapter.GateViewHolder> {

    public interface OnGateClickListener {
        void onGateToggle(Gate gate, int position);
    }

    private final List<Gate> gates;
    private final OnGateClickListener listener;

    public GateAdapter(List<Gate> gates, OnGateClickListener listener) {
        this.gates = gates;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gate, parent, false);
        return new GateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GateViewHolder holder, int position) {
        Gate gate = gates.get(position);

        holder.tvGateNumber.setText(gate.getGateNumber().split("e")[0] + "e " + gate.getGateNumber().split("e")[1]);
        holder.tvActionStatus.setText(gate.getActionStatus());

        if (gate.isOpen()) {
            holder.tvGateStatus.setText("Open");
            holder.tvGateStatus.setBackgroundResource(R.drawable.status_badge_open);
            holder.btnToggleGate.setText("CLOSE GATE");
            holder.btnToggleGate.setBackgroundTintList(
                    ContextCompat.getColorStateList(holder.itemView.getContext(), android.R.color.holo_red_dark)
            );
        } else {
            holder.tvGateStatus.setText("Closed");
            holder.tvGateStatus.setBackgroundResource(R.drawable.status_badge_closed);
            holder.btnToggleGate.setText("OPEN GATE");
            holder.btnToggleGate.setBackgroundTintList(
                    ContextCompat.getColorStateList(holder.itemView.getContext(), android.R.color.holo_green_dark)
            );
        }

        boolean busy = gate.getActionStatus().contains("Opening") || gate.getActionStatus().contains("Closing");
        holder.btnToggleGate.setEnabled(!busy && gate.isClickable());

        holder.btnToggleGate.setOnClickListener(v -> {
            if (!busy && gate.isClickable()) {
                listener.onGateToggle(gate, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return gates.size();
    }

    public void updateGate(int position, Gate gate) {
        gates.set(position, gate);
        notifyItemChanged(position);
    }

    static class GateViewHolder extends RecyclerView.ViewHolder {
        TextView tvGateNumber, tvGateStatus, tvActionStatus;
        Button btnToggleGate;

        public GateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGateNumber = itemView.findViewById(R.id.tvGateNumber);
            tvGateStatus = itemView.findViewById(R.id.tvGateStatus);
            tvActionStatus = itemView.findViewById(R.id.tvActionStatus);
            btnToggleGate = itemView.findViewById(R.id.btnToggleGate);
        }
    }
}
