package com.ds.deliveryapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.deliveryapp.R;
import com.ds.deliveryapp.clients.res.DeliverySession;
import com.ds.deliveryapp.utils.StatusMapper;
import com.ds.deliveryapp.utils.FormaterUtil;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {

    public interface OnSessionClickListener {
        void onSessionClick(DeliverySession session);
    }

    private List<DeliverySession> sessionList = new ArrayList<>();
    private final OnSessionClickListener listener;

    public SessionAdapter(OnSessionClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<DeliverySession> sessions) {
        this.sessionList.clear();
        if (sessions != null) {
            this.sessionList.addAll(sessions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeliverySession session = sessionList.get(position);
        holder.bind(session, listener);
    }

    @Override
    public int getItemCount() {
        return sessionList != null ? sessionList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvSessionId;
        TextView tvSessionStatus;
        TextView tvStartTime;
        TextView tvEndTime;
        TextView tvTotalTasks;
        TextView tvCompletedTasks;
        TextView tvFailedTasks;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvSessionId = itemView.findViewById(R.id.tv_session_id);
            tvSessionStatus = itemView.findViewById(R.id.tv_session_status);
            tvStartTime = itemView.findViewById(R.id.tv_start_time);
            tvEndTime = itemView.findViewById(R.id.tv_end_time);
            tvTotalTasks = itemView.findViewById(R.id.tv_total_tasks);
            tvCompletedTasks = itemView.findViewById(R.id.tv_completed_tasks);
            tvFailedTasks = itemView.findViewById(R.id.tv_failed_tasks);
        }

        void bind(DeliverySession session, OnSessionClickListener listener) {
            // Session ID (shortened)
            String sessionIdStr = session.getId() != null ? session.getId().toString() : "N/A";
            if (sessionIdStr.length() > 8) {
                sessionIdStr = sessionIdStr.substring(0, 8) + "...";
            }
            tvSessionId.setText("Phiên: " + sessionIdStr);

            // Status
            String statusText = StatusMapper.mapSessionStatus(session.getStatus());
            tvSessionStatus.setText(statusText);
            
            // Set status background color for status badge
            int statusColor = R.color.status_pending;
            if ("COMPLETED".equalsIgnoreCase(session.getStatus())) {
                statusColor = R.color.status_success;
            } else if ("FAILED".equalsIgnoreCase(session.getStatus())) {
                statusColor = R.color.status_failed;
            } else if ("IN_PROGRESS".equalsIgnoreCase(session.getStatus())) {
                statusColor = R.color.status_on_route;
            }
            tvSessionStatus.setBackgroundColor(
                    ContextCompat.getColor(itemView.getContext(), statusColor)
            );
            
            // Set card background color based on session status (light colors)
            int cardBgColor = R.color.session_bg_created; // Default for CREATED
            String sessionStatus = session.getStatus();
            if (sessionStatus != null) {
                if ("COMPLETED".equalsIgnoreCase(sessionStatus)) {
                    cardBgColor = R.color.session_bg_completed;
                } else if ("FAILED".equalsIgnoreCase(sessionStatus)) {
                    cardBgColor = R.color.session_bg_failed;
                } else if ("IN_PROGRESS".equalsIgnoreCase(sessionStatus)) {
                    cardBgColor = R.color.session_bg_in_progress;
                } else if ("CREATED".equalsIgnoreCase(sessionStatus)) {
                    cardBgColor = R.color.session_bg_created;
                }
            }
            cardView.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.getContext(), cardBgColor)
            );

            // Start time
            if (session.getStartTime() != null && !session.getStartTime().isEmpty()) {
                tvStartTime.setText("Bắt đầu: " + FormaterUtil.formatDateTime(session.getStartTime()));
            } else {
                tvStartTime.setText("Bắt đầu: --");
            }

            // End time
            if (session.getEndTime() != null && !session.getEndTime().isEmpty()) {
                tvEndTime.setText("Kết thúc: " + FormaterUtil.formatDateTime(session.getEndTime()));
            } else {
                tvEndTime.setText("Kết thúc: --");
            }

            // Task counts
            tvTotalTasks.setText(String.valueOf(session.getTotalTasks()));
            tvCompletedTasks.setText(String.valueOf(session.getCompletedTasks()));
            tvFailedTasks.setText(String.valueOf(session.getFailedTasks()));

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSessionClick(session);
                }
            });
        }
    }
}
