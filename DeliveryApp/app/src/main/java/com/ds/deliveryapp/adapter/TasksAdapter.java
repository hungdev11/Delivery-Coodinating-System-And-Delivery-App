package com.ds.deliveryapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.deliveryapp.R;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.utils.FormaterUtil;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onOrderClick(DeliveryAssignment task);
    }

    private List<DeliveryAssignment> taskList;
    private OnTaskClickListener listener;

    public TasksAdapter(List<DeliveryAssignment> taskList, OnTaskClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // *** Dùng layout item_task.xml mới ***
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        DeliveryAssignment task = taskList.get(position);

        // Ánh xạ dữ liệu vào layout tối ưu item_task_driver_optimized.xml
        holder.tvParcelCode.setText("Mã đơn: " + task.getParcelCode());
        holder.tvDeliveryLocation.setText(task.getDeliveryLocation());
        // Giả định bạn có phương thức helper để định dạng tiền tệ và khoảng cách
        holder.tvParcelValue.setText(FormaterUtil.formatCurrency(task.getValue()));
        holder.tvStatus.setText(task.getStatus() != null ? mapStatus(task.getStatus().toUpperCase()) : "MỚI");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(task);
        });
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        //item_task.xml
        TextView tvParcelCode, tvDeliveryLocation, tvParcelValue, tvStatus;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvParcelCode = itemView.findViewById(R.id.tv_parcel_code);
            tvDeliveryLocation = itemView.findViewById(R.id.tv_delivery_location);
            tvParcelValue = itemView.findViewById(R.id.tv_parcel_value);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }

    private String formatDistanceM(double meters) {
        if (meters < 1000) return Math.round(meters) + " m";
        return String.format("%.1f km", meters / 1000);
    }

    private String formatDurationS(long seconds) {
        long minutes = seconds / 60;
        return "~" + minutes + " phút";
    }

    public void updateTasks(List<DeliveryAssignment> newTasks) {
        this.taskList = newTasks;
        notifyDataSetChanged();
    }

    private String mapStatus (String status) {
        return switch (status) {
            case "PROCESSING" -> "ĐANG XỬ LÝ";
            case "COMPLETED" -> "ĐÃ HOÀN THÀNH";
            case "FAILED" -> "THẤT BẠI";
            default -> throw new IllegalStateException("Unexpected value: " + status);
        };
    }
}