package com.ds.deliveryapp;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
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
import com.ds.deliveryapp.model.IssueReason;
import com.ds.deliveryapp.utils.FormaterUtil;
import com.ds.deliveryapp.utils.TaskActionHandler;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity implements TaskActionHandler.TaskUpdateListener{
    private TextView tvParcelCode, tvStatus, tvReceiverName, tvDeliveryLocation;
    private Button btnCallReceiver, btnMainAction, btnFailAction;
    private TextView tvParcelValue;
    // View từ card_details_and_route_info.xml (included)
    private TextView tvDeliveryType, tvWeight, tvParcelId;
    private TextView tvCreatedAt, tvCompletedAt, tvFailReason;
    private LinearLayout layoutCompletedAt, layoutFailReason;
    private DeliveryAssignment currentTask;
    private TaskActionHandler actionHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        initViews();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("TASK_DETAIL")) {
            currentTask = (DeliveryAssignment) intent.getSerializableExtra("TASK_DETAIL");

            if (currentTask != null) {
                // 💡 KHỞI TẠO HANDLER
                actionHandler = new TaskActionHandler(this, this);

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
        btnFailAction = findViewById(R.id.btn_fail_action);
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
        layoutCompletedAt.setVisibility(GONE);
        layoutFailReason.setVisibility(GONE);
    }

    private void displayData(DeliveryAssignment task) {
        // CARD 1: HEADER & ACTION
        if (tvParcelCode != null) tvParcelCode.setText(task.getParcelCode());
        if (tvStatus != null) tvStatus.setText(task.getStatus() != null ? task.getStatus().toUpperCase() : "N/A");
        if (tvReceiverName != null) tvReceiverName.setText(task.getReceiverName() != null ? task.getReceiverName() : "Khách hàng");
        if (tvDeliveryLocation != null) tvDeliveryLocation.setText("Địa chỉ: " + task.getDeliveryLocation());

        // CARD 2: COD
        if (tvParcelValue != null) tvParcelValue.setText(FormaterUtil.formatCurrency(task.getValue()));

        // CARD 3/4: CHI TIẾT & VẬN HÀNH
        if (tvDeliveryType != null) tvDeliveryType.setText(DeliveryType.NORMAL.equals(task.getDeliveryType()) ? "Giao Hàng Tiêu Chuẩn" : "Giao Hàng Nhanh");
        if (tvWeight != null) tvWeight.setText(formatWeight(task.getWeight()));
        if (tvParcelId != null) tvParcelId.setText(task.getParcelCode());

        String formatCreatedAt = FormaterUtil.formatDateTime(task.getCreatedAt());
        String formatCompletedAt = FormaterUtil.formatDateTime(task.getCompletedAt());

        if (tvCreatedAt != null) tvCreatedAt.setText(formatCreatedAt);

        if (layoutCompletedAt != null) {
            boolean isCompleted = formatCompletedAt != null &&
                    !formatCompletedAt.isEmpty() &&
                    !formatCompletedAt.equals(formatCreatedAt);

            if (isCompleted) {
                layoutCompletedAt.setVisibility(VISIBLE);
                if (tvCompletedAt != null) {
                    tvCompletedAt.setText(formatCompletedAt);
                }
            } else {
                layoutCompletedAt.setVisibility(GONE);
            }
        }

        if (layoutFailReason != null) {
            if (task.getFailReason() != null && !task.getFailReason().isEmpty()) {
                layoutFailReason.setVisibility(VISIBLE);
                if (tvFailReason != null) tvFailReason.setText(task.getFailReason());
            } else {
                layoutFailReason.setVisibility(GONE);
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
            case "SUCCESS":
            case "FAILED":
                btnMainAction.setText("ĐÃ HOÀN TẤT");
                btnMainAction.setEnabled(false);
                btnFailAction.setVisibility(GONE);
                btnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gray));
                break;
            default:
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

        if (btnFailAction != null) {
            btnFailAction.setOnClickListener(v -> {
                if (currentTask != null) {
                    actionHandler.startFailureFlow(currentTask);
                }
            });
        }

        if (btnMainAction != null) {
            btnMainAction.setOnClickListener(v -> {
                if (currentTask != null && btnMainAction.isEnabled()) {
                    actionHandler.startCompletionFlow(currentTask);
                }
            });
        }
    }

    @Override
    public void onStatusUpdated(String newStatus) {
        currentTask.setStatus(newStatus);
        updateMainActionButton(newStatus);
    }
}
