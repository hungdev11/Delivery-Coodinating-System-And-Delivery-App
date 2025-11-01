package com.ds.deliveryapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.deliveryapp.R;
import com.ds.deliveryapp.enums.ParcelStatus;
import com.ds.deliveryapp.model.Parcel;
import com.ds.deliveryapp.utils.FormaterUtil;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

public class ParcelAdapter extends RecyclerView.Adapter<ParcelAdapter.ViewHolder> {

    // 1. Interface để xử lý click
    public interface OnItemClickListener {
        void onItemClick(Parcel parcel);
    }
    private List<Parcel> parcelList = new ArrayList<>();
    private final OnItemClickListener listener;

    // 3. Constructor
    public ParcelAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * 4. Phương thức cập nhật dữ liệu
     */
    public void setData(List<Parcel> newParcelList) {
        this.parcelList.clear();
        if (newParcelList != null) {
            this.parcelList.addAll(newParcelList);
        }
        notifyDataSetChanged();
    }


    // 5. ViewHolder chứa logic binding cho một item (dùng findViewById)
    public static class ViewHolder extends RecyclerView.ViewHolder {

        // Định dạng ngày tháng
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Khai báo các View
        private final TextView textParcelCode;
        private final TextView textParcelDestination;
        private final TextView textParcelDate;
        private final TextView textParcelStatus;
        private final Context context; // <-- Thêm Context để lấy màu

        public ViewHolder(View itemView) {
            super(itemView);
            this.context = itemView.getContext(); // <-- Lưu context

            // Ánh xạ View bằng findViewById
            textParcelCode = itemView.findViewById(R.id.text_parcel_code);
            textParcelDestination = itemView.findViewById(R.id.text_parcel_destination);
            textParcelDate = itemView.findViewById(R.id.text_parcel_date);
            textParcelStatus = itemView.findViewById(R.id.text_parcel_status);
        }

        // Bind dữ liệu vào ViewHolder
        public void bind(Parcel parcel, OnItemClickListener listener) {
            textParcelCode.setText(parcel.getCode());
            textParcelDestination.setText("Đến: " + parcel.getTargetDestination());
            textParcelDate.setText("Ngày tạo: " + FormaterUtil.formatDateTime(parcel.getCreatedAt()));

            // Xử lý trạng thái (text)
            if (parcel.getStatus() != null) {
                textParcelStatus.setText(parcel.getStatus().name());
                // Set màu cho chữ (hoặc background)
                textParcelStatus.setTextColor(
                        ContextCompat.getColor(context, getStatusColor(parcel.getStatus()))
                );
            } else {
                textParcelStatus.setText("N/A");
                textParcelStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.default_text_color)
                );
            }

            // Gán sự kiện click cho toàn bộ item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(parcel);
                }
            });
        }

        // Helper để lấy màu dựa trên trạng thái
        private int getStatusColor(ParcelStatus status) {
            switch (status) {
                case IN_WAREHOUSE:
                case DELAYED:
                    return R.color.status_pending;
                case ON_ROUTE:
                    return R.color.status_on_route;
                case DELIVERED:
                case SUCCEEDED:
                    return R.color.status_success;
                case FAILED:
                case DISPUTE:
                case LOST:
                    return R.color.status_failed;
                default:
                    return R.color.default_text_color;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parcel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Parcel parcel = parcelList.get(position);
        holder.bind(parcel, listener);
    }

    @Override
    public int getItemCount() {
        return parcelList != null ? parcelList.size() : 0;
    }
}

