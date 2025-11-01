package com.ds.deliveryapp;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.ds.deliveryapp.utils.FormaterUtil.formatWeight;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ds.deliveryapp.enums.DeliveryType;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.utils.FormaterUtil;
import com.ds.deliveryapp.utils.TaskActionHandler;


public class TaskDetailActivity extends AppCompatActivity implements TaskActionHandler.TaskUpdateListener{
    private TextView tvParcelCode, tvStatus, tvReceiverName, tvDeliveryLocation;
    private Button btnCallReceiver, btnMainAction, btnFailAction, btnChatReceiver;
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
        btnChatReceiver = findViewById(R.id.btn_chat_receiver_detail);
        tvDeliveryType = findViewById(R.id.tv_delivery_type);
        tvWeight = findViewById(R.id.tv_weight);
        tvParcelId = findViewById(R.id.tv_parcel_id);
        tvCreatedAt = findViewById(R.id.tv_created_at);
        tvCompletedAt = findViewById(R.id.tv_completed_at);
        tvFailReason = findViewById(R.id.tv_fail_reason);
        layoutCompletedAt = findViewById(R.id.layout_completed_at);
        layoutFailReason = findViewById(R.id.layout_fail_reason);
        layoutCompletedAt.setVisibility(GONE);
        layoutFailReason.setVisibility(GONE);
    }

    private void displayData(DeliveryAssignment task) {
        if (tvParcelCode != null) tvParcelCode.setText(task.getParcelCode());
        if (tvStatus != null) tvStatus.setText(task.getStatus() != null ? task.getStatus().toUpperCase() : "N/A");
        if (tvReceiverName != null) tvReceiverName.setText(task.getReceiverName() != null ? task.getReceiverName() : "Khách hàng");
        if (tvDeliveryLocation != null) tvDeliveryLocation.setText("Địa chỉ: " + task.getDeliveryLocation());
        if (tvParcelValue != null) tvParcelValue.setText(FormaterUtil.formatCurrency(task.getValue()));
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
        updateMainActionButton(task.getStatus());
    }

    private void updateMainActionButton(String status) {
        int green = getResources().getColor(android.R.color.holo_green_dark);
        int gray = getResources().getColor(android.R.color.darker_gray);
        if (btnMainAction == null) return;
        switch (status) {
            case "IN_PROGRESS":
                btnMainAction.setText("HOÀN TẤT GIAO HÀNG");
                btnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green));
                break;
            case "COMPLETED":
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
        if (btnCallReceiver != null) {
            btnCallReceiver.setOnClickListener(v -> {
                String phone = "0935960974"; // task.getReceiverPhone();
                if (phone != null && !phone.isEmpty()) {
                    String anonymousPhone = "#31#" + phone;

                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + anonymousPhone));
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

        btnChatReceiver.setOnClickListener(v -> {
            if (currentTask == null) {
                // (Xử lý lỗi nếu data chưa sẵn sàng)
                Toast.makeText(this, "Không tìm thấy thông tin người nhận", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Tạo Intent để mở ChatActivity
            Intent chatIntent = new Intent(TaskDetailActivity.this, ChatActivity.class);

            // 2. Đóng gói (put) dữ liệu được yêu cầu

            chatIntent.putExtra("RECIPIENT_ID", currentTask.getReceiverId());
            chatIntent.putExtra("RECIPIENT_NAME", currentTask.getReceiverName());
            // Dữ liệu MỚI cho thanh tiêu đề (theo yêu cầu)
            chatIntent.putExtra("PARCEL_CODE", currentTask.getParcelCode());
            chatIntent.putExtra("PARCEL_ID", currentTask.getParcelId());
            // 3. Khởi chạy ChatActivity
            startActivity(chatIntent);
        });
    }

    /**
     * Xử lý kết quả trả về từ Camera Intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TaskActionHandler.REQUEST_IMAGE_CAPTURE) {
            actionHandler.handleActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Xử lý kết quả trả về từ HỎI QUYỀN
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == TaskActionHandler.REQUEST_CAMERA_PERMISSION) {
            actionHandler.handlePermissionResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public void onStatusUpdated(String newStatus) {
        currentTask.setStatus(newStatus);
        updateMainActionButton(newStatus);

        Intent resultIntent = new Intent();
        setResult(Activity.RESULT_OK, resultIntent);

        displayData(currentTask);

        Toast.makeText(this, "Đã cập nhật: " + newStatus, Toast.LENGTH_SHORT).show();
        finish();
    }
}

