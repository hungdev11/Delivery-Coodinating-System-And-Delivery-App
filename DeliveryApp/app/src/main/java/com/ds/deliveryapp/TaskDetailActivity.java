package com.ds.deliveryapp;

import static com.ds.deliveryapp.utils.FormaterUtil.formatDistanceM;
import static com.ds.deliveryapp.utils.FormaterUtil.formatDurationS;
import static com.ds.deliveryapp.utils.FormaterUtil.formatWeight;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ds.deliveryapp.enums.DeliveryType;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.utils.FormaterUtil;

import java.text.NumberFormat;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {
    private TextView tvParcelCode, tvStatus, tvReceiverName, tvReceiverPhone, tvDeliveryLocation;
    private Button btnCallReceiver, btnOpenMap, btnMainAction;
    private TextView tvParcelValue;

    // View từ card_details_and_route_info.xml (included)
    private TextView tvDeliveryType, tvWeight, tvParcelId;
    private TextView tvCreatedAt, tvCompletedAt, tvFailReason;
    private LinearLayout layoutCompletedAt, layoutFailReason;

    private DeliveryAssignment currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        initViews();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("TASK_DETAIL")) {
            currentTask = (DeliveryAssignment) intent.getSerializableExtra("TASK_DETAIL");

            if (currentTask != null) {
                displayData(currentTask);
                setupEventListeners(currentTask);
            } else {
                Toast.makeText(this, "Lỗi tải dữ liệu chi tiết.", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Không tìm thấy dữ liệu đơn hàng.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        tvParcelCode = findViewById(R.id.tv_parcel_code);
        tvStatus = findViewById(R.id.tv_status);
        tvReceiverName = findViewById(R.id.tv_receiver_name_detail);
        tvDeliveryLocation = findViewById(R.id.tv_delivery_location_detail);
        tvParcelValue = findViewById(R.id.tv_parcel_value_detail);

        btnCallReceiver = findViewById(R.id.btn_call_receiver_detail);
        btnOpenMap = findViewById(R.id.btn_open_map_detail);
        btnMainAction = findViewById(R.id.btn_main_action);

        // Ánh xạ View từ card_details_and_route_info.xml
        tvDeliveryType = findViewById(R.id.tv_delivery_type);
        tvWeight = findViewById(R.id.tv_weight);
        tvParcelId = findViewById(R.id.tv_parcel_id);
        tvCreatedAt = findViewById(R.id.tv_created_at);
        tvCompletedAt = findViewById(R.id.tv_completed_at);
        tvFailReason = findViewById(R.id.tv_fail_reason);
        layoutCompletedAt = findViewById(R.id.layout_completed_at);
        layoutFailReason = findViewById(R.id.layout_fail_reason);

        // Đặt trạng thái ban đầu
        layoutCompletedAt.setVisibility(View.GONE);
        layoutFailReason.setVisibility(View.GONE);
    }

    private void displayData(DeliveryAssignment task) {
        // CARD 1: HEADER & ACTION
        if (tvParcelCode != null) tvParcelCode.setText(task.getParcelCode());
        if (tvStatus != null) tvStatus.setText(task.getStatus() != null ? task.getStatus().toUpperCase() : "N/A");
        if (tvReceiverName != null) tvReceiverName.setText(task.getReceiverName() != null ? task.getReceiverName() : "Khách hàng");
        if (tvReceiverPhone != null) tvReceiverPhone.setText(task.getReceiverPhone());
        if (tvDeliveryLocation != null) tvDeliveryLocation.setText("Địa chỉ: " + task.getDeliveryLocation());

        // CARD 2: COD
        if (tvParcelValue != null) tvParcelValue.setText(FormaterUtil.formatCurrency(task.getValue()));

        // CARD 3/4: CHI TIẾT & VẬN HÀNH
        if (tvDeliveryType != null) tvDeliveryType.setText(DeliveryType.NORMAL.equals(task.getDeliveryType()) ? "Giao Hàng Tiêu Chuẩn" : "Giao Hàng Nhanh");
        if (tvWeight != null) tvWeight.setText(formatWeight(task.getWeight()));
        if (tvParcelId != null) tvParcelId.setText(task.getParcelId());

        if (tvCreatedAt != null) tvCreatedAt.setText(task.getCreatedAt());

        // HIỂN THỊ CÓ ĐIỀU KIỆN
        if (layoutCompletedAt != null) {
            if (task.getCompletedAt() != null && !task.getCompletedAt().isEmpty()) {
                layoutCompletedAt.setVisibility(View.VISIBLE);
                if (tvCompletedAt != null) tvCompletedAt.setText(task.getCompletedAt());
            } else {
                layoutCompletedAt.setVisibility(View.GONE);
            }
        }

        if (layoutFailReason != null) {
            if (task.getFailReason() != null && !task.getFailReason().isEmpty()) {
                layoutFailReason.setVisibility(View.VISIBLE);
                if (tvFailReason != null) tvFailReason.setText(task.getFailReason());
            } else {
                layoutFailReason.setVisibility(View.GONE);
            }
        }

        // LOGIC NÚT HÀNH ĐỘNG CHÍNH
        updateMainActionButton(task.getStatus());
    }

    private void updateMainActionButton(String status) {
        int green = getResources().getColor(android.R.color.holo_green_dark);
        int gray = getResources().getColor(android.R.color.darker_gray);

        if (btnMainAction == null) return;

        switch (status) {
            case "PROCESSING":
                btnMainAction.setText("HOÀN TẤT GIAO HÀNG");
                btnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green));
                break;
            case "COMPLETED":
            case "FAILED":
                btnMainAction.setText("ĐÃ HOÀN TẤT");
                btnMainAction.setEnabled(false);
                btnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gray));
                break;
            default:
                btnMainAction.setText("HÀNH ĐỘNG");
                btnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gray));
                break;
        }
    }

    private void setupEventListeners(DeliveryAssignment task) {
        // Nút Gọi khách hàng
        if (btnCallReceiver != null) {
            btnCallReceiver.setOnClickListener(v -> {
                String phone = "0935960974";
//                        task.getReceiverPhone();
                if (phone != null && !phone.isEmpty()) {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                    startActivity(dialIntent);
                } else {
                    Toast.makeText(this, "Không có số điện thoại khách hàng", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Nút Chỉ Đường
        if (btnOpenMap != null) {
            btnOpenMap.setOnClickListener(v -> {
                String location = task.getDeliveryLocation();
                if (location != null && !location.isEmpty()) {
                    Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        Toast.makeText(this, "Không tìm thấy ứng dụng bản đồ Google Maps", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }


        // Nút hành động chính (Đã đến / Hoàn thành)
        if (btnMainAction != null) {
            btnMainAction.setOnClickListener(v -> {
                if (currentTask != null && btnMainAction.isEnabled()) {
                    String newStatus = getNextStatus(currentTask.getStatus());
                    if (newStatus != null) {
                        Toast.makeText(this, "Yêu cầu cập nhật trạng thái lên: " + newStatus, Toast.LENGTH_SHORT).show();
                        currentTask.setStatus(newStatus);
                        updateMainActionButton(newStatus);
                    } else {
                        Toast.makeText(this, "Không có hành động tiếp theo cho trạng thái này.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private String getNextStatus(String currentStatus) {
        switch (currentStatus) {
            case "IN_TRANSIT":
                return "COMPLETED";
            default:
                return null;
        }
    }
}
