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

public class TasksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(DeliveryAssignment task);
    }

    private static final int VIEW_TYPE_TASK = 0;
    private static final int VIEW_TYPE_SKELETON = 1;

    private List<DeliveryAssignment> taskList;
    private OnTaskClickListener listener;
    private boolean showSkeleton = false;

    public TasksAdapter(List<DeliveryAssignment> taskList, OnTaskClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (showSkeleton && position < getSkeletonCount()) {
            return VIEW_TYPE_SKELETON;
        }
        return VIEW_TYPE_TASK;
    }

    private int getSkeletonCount() {
        return showSkeleton ? 5 : 0; // Show 5 skeleton items
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SKELETON) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task_skeleton, parent, false);
            return new SkeletonViewHolder(view);
        } else {
            // *** Dùng layout item_task.xml mới ***
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SkeletonViewHolder) {
            // Skeleton view doesn't need binding
            return;
        }

        TaskViewHolder taskHolder = (TaskViewHolder) holder;
        int taskPosition = showSkeleton ? position - getSkeletonCount() : position;
        
        if (taskPosition < 0 || taskList == null || taskPosition >= taskList.size()) {
            return;
        }

        DeliveryAssignment task = taskList.get(taskPosition);

        // Ánh xạ dữ liệu vào layout tối ưu item_task_driver_optimized.xml
        taskHolder.tvParcelCode.setText("Mã đơn: " + task.getParcelCode());
        taskHolder.tvDeliveryLocation.setText(task.getDeliveryLocation());
        // Giả định bạn có phương thức helper để định dạng tiền tệ và khoảng cách
        taskHolder.tvParcelValue.setText(FormaterUtil.formatCurrency(task.getValue()));
        taskHolder.tvStatus.setText(task.getStatus() != null ? mapStatus(task.getStatus().toUpperCase()) : "MỚI");

        taskHolder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(task);
        });
    }

    @Override
    public int getItemCount() {
        int taskCount = taskList != null ? taskList.size() : 0;
        return taskCount + getSkeletonCount();
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

    static class SkeletonViewHolder extends RecyclerView.ViewHolder {
        public SkeletonViewHolder(@NonNull View itemView) {
            super(itemView);
            // Skeleton view doesn't need any binding
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

    public void setShowSkeleton(boolean show) {
        if (this.showSkeleton != show) {
            this.showSkeleton = show;
            notifyDataSetChanged();
        }
    }

    private String mapStatus (String status) {
        return switch (status) {
            case "IN_PROGRESS" -> "ĐANG XỬ LÝ";
            case "COMPLETED" -> "ĐÃ HOÀN THÀNH";
            case "FAILED" -> "THẤT BẠI";
            default -> throw new IllegalStateException("Unexpected value: " + status);
        };
    }
}
