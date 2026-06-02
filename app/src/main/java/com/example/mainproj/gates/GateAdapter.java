package com.example.mainproj.gates;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.example.mainproj.auth.SessionManager;

import java.util.List;

public class GateAdapter extends RecyclerView.Adapter<GateAdapter.GateViewHolder> {

    public interface OnGateClickListener {
        void onGateToggle(Gate gate, int position);
        void onGateAdminSettings(Gate gate, int position);
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

        String[] parts = gate.getGateNumber().split("e");
        String displayName = parts.length >= 2 ? parts[0] + "e " + parts[1] : gate.getGateNumber();
        holder.tvGateNumber.setText(displayName);
        holder.tvActionStatus.setText(gate.getActionStatus());

        if (gate.isDisabled()) {
            holder.tvGateStatus.setText("Disabled");
            holder.tvGateStatus.setBackgroundResource(R.drawable.status_badge_closed);
            holder.btnToggleGate.setText("DISABLED");
            holder.btnToggleGate.setBackgroundTintList(
                    ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.colorTextHint)
            );
        } else if (gate.isOpen()) {
            holder.tvGateStatus.setText("Open");
            holder.tvGateStatus.setBackgroundResource(R.drawable.status_badge_open);
            holder.btnToggleGate.setText("CLOSE GATE");
            holder.btnToggleGate.setBackgroundTintList(
                    ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.colorStatusDeclined)
            );
        } else {
            holder.tvGateStatus.setText("Closed");
            holder.tvGateStatus.setBackgroundResource(R.drawable.status_badge_closed);
            holder.btnToggleGate.setText("OPEN GATE");
            holder.btnToggleGate.setBackgroundTintList(
                    ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.colorStatusAccepted)
            );
        }

        boolean busy = gate.getActionStatus().contains("Opening") || gate.getActionStatus().contains("Closing");

        // checks if user can view (visibility + disabled flag + allowedUsers)
        if (gate.isDisabled() && SessionManager.getRoleLevel() < 2) {
            holder.itemView.setVisibility(View.GONE);
            return;
        }
        boolean isExplicitlyAllowed = gate.getAllowedUsers() != null
                && Boolean.TRUE.equals(gate.getAllowedUsers().get(SessionManager.getCurrentUsername()));
        if (gate.getVisibilityLevel() == -1) {
            if (SessionManager.getRoleLevel() < 2 && !isExplicitlyAllowed) {
                holder.itemView.setVisibility(View.GONE);
                return;
            }
        } else if (!isExplicitlyAllowed && gate.getVisibilityLevel() > SessionManager.getRoleLevel()) {
            holder.itemView.setVisibility(View.GONE);
            return;
        }
        holder.itemView.setVisibility(View.VISIBLE);

        holder.btnToggleGate.setEnabled(!busy && gate.isClickable());

        // Admin gear icon: shown for roleLevel >= 1 always
        if (gate.getControlLevel() == -1) {
            boolean canAdmin = SessionManager.isSuperAdmin()
                    || (gate.getAllowedAdmins() != null
                    && Boolean.TRUE.equals(gate.getAllowedAdmins().get(SessionManager.getCurrentUsername())));
            holder.btnGateAdminSettings.setVisibility(canAdmin ? View.VISIBLE : View.GONE);
        } else if (SessionManager.getRoleLevel() >= gate.getControlLevel() && SessionManager.getRoleLevel() != 0) {
            holder.btnGateAdminSettings.setVisibility(View.VISIBLE);
        } else {
            holder.btnGateAdminSettings.setVisibility(View.GONE);
        }

        holder.btnToggleGate.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Gate g = gates.get(pos);
            if (g.isClickable()) listener.onGateToggle(g, pos);
        });

        holder.btnGateAdminSettings.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            listener.onGateAdminSettings(gates.get(pos), pos);
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
        ImageButton btnGateAdminSettings;

        public GateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGateNumber = itemView.findViewById(R.id.tvGateNumber);
            tvGateStatus = itemView.findViewById(R.id.tvGateStatus);
            tvActionStatus = itemView.findViewById(R.id.tvActionStatus);
            btnToggleGate = itemView.findViewById(R.id.btnToggleGate);
            btnGateAdminSettings = itemView.findViewById(R.id.btnGateAdminSettings);
        }
    }
}
