package com.ds.deliveryapp;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.ds.deliveryapp.utils.FormaterUtil.formatWeight;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ds.deliveryapp.adapters.ProofAdapter;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.enums.DeliveryType;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.model.DeliveryProof;
import com.ds.deliveryapp.utils.FormaterUtil;
import com.ds.deliveryapp.utils.SessionManager;
import com.ds.deliveryapp.utils.TaskActionHandler;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class TaskDetailActivity extends AppCompatActivity implements TaskActionHandler.TaskUpdateListener{
    private TextView tvParcelCode, tvStatus, tvReceiverName, tvDeliveryLocation;
    private Button btnCallReceiver, btnMainAction, btnFailAction, btnChatReceiver, btnReturnToWarehouse, btnAcceptTask;
    private TextView tvParcelValue;

    private static final int REQUEST_CODE_PROOF = 9001;
    private static final int REQUEST_CODE_ACCEPT_TASK = 9003; // Mã request mới cho việc nhận task

    // View từ card_details_and_route_info.xml (included)
    private TextView tvDeliveryType, tvWeight, tvParcelId;
    private TextView tvCreatedAt, tvCompletedAt, tvFailReason;
    private LinearLayout layoutCompletedAt, layoutFailReason;

    // Proofs section
    private CardView cardProofs;
    private RecyclerView recyclerProofs;
    private TextView tvProofsLoading, tvProofsEmpty;
    private ProofAdapter proofAdapter;
    private List<DeliveryProof> currentProofs = new ArrayList<>();

    private DeliveryAssignment currentTask;
    private TaskActionHandler actionHandler;
    private String sessionStatus; // CREATED, IN_PROGRESS, etc.
    private SessionClient sessionClient;
    private SessionManager sessionManager; // Thêm SessionManager để lấy DriverId

    private boolean hasUnfinishedTasks;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        // Khởi tạo SessionManager
        sessionManager = new SessionManager(this);

        initViews();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("TASK_DETAIL")) {
            currentTask = (DeliveryAssignment) intent.getSerializableExtra("TASK_DETAIL");
            sessionStatus = intent.getStringExtra("SESSION_STATUS");
            hasUnfinishedTasks = getIntent()
                    .getBooleanExtra("HAS_UNFINISHED_TASKS", false);

            if (currentTask != null) {
                actionHandler = new TaskActionHandler(this, this);
                displayData(currentTask);
                setupEventListeners(currentTask);
                updateButtonsBasedOnSessionStatus();
                loadProofs(currentTask.getAssignmentId());
            } else {
                Toast.makeText(this, "Lỗi tải dữ liệu chi tiết.", Toast.LENGTH_LONG).show();
                finish();
            }

            if (sessionStatus == null || currentTask.getStatus().equals("COMPLETED")) {
                btnMainAction.setVisibility(GONE);
                btnFailAction.setVisibility(GONE);
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
        btnReturnToWarehouse = findViewById(R.id.btn_return_to_warehouse);
        btnAcceptTask = findViewById(R.id.btn_scan_verify);
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

        // Proofs section
        cardProofs = findViewById(R.id.card_proofs);
        recyclerProofs = findViewById(R.id.recycler_proofs);
        tvProofsLoading = findViewById(R.id.tv_proofs_loading);
        tvProofsEmpty = findViewById(R.id.tv_proofs_empty);

        // Setup RecyclerView for proofs
        proofAdapter = new ProofAdapter(this);
        recyclerProofs.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerProofs.setAdapter(proofAdapter);

        // Initialize API client
        sessionClient = RetrofitClient.getRetrofitInstance(this).create(SessionClient.class);
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

        cardProofs.setVisibility(GONE);

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
        updateReturnToWarehouseButton(task);

        updateBottomButtonsByTaskStatus(task);
    }

    private boolean hasReturnedProof(List<DeliveryProof> proofs) {
        if (proofs == null) return false;
        for (DeliveryProof proof : proofs) {
            if ("RETURNED".equalsIgnoreCase(proof.getType())) {
                return true;
            }
        }
        return false;
    }


    private void updateReturnToWarehouseButton(DeliveryAssignment task) {
        if (btnReturnToWarehouse == null || task == null) return;

        boolean isReturnState =
                "FAILED".equals(task.getStatus()) || "DELAYED".equals(task.getStatus());

        if (!isReturnState) {
            btnReturnToWarehouse.setVisibility(GONE);
            return;
        }

        // 👉 Nếu đã có proof RETURNED → disable
        if (hasReturnedProof(currentProofs)) {
            btnReturnToWarehouse.setVisibility(VISIBLE);
            btnReturnToWarehouse.setEnabled(false);
            btnReturnToWarehouse.setText("ĐÃ TRẢ HÀNG VỀ KHO");
            return;
        }

        // 👉 Chưa có proof → cho phép trả kho
        btnReturnToWarehouse.setVisibility(VISIBLE);
        btnReturnToWarehouse.setEnabled(true);
        btnReturnToWarehouse.setText("TRẢ HÀNG VỀ KHO");

        btnReturnToWarehouse.setOnClickListener(v -> {
            if (hasUnfinishedTasks) {
                new AlertDialog.Builder(this)
                        .setTitle("Chưa thể trả hàng")
                        .setMessage(
                                "Vẫn còn đơn hàng đang giao.\n" +
                                        "Chỉ được trả hàng về kho khi tất cả các đơn còn lại đều bị trễ hoặc thất bại."
                        )
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            Intent intent = new Intent(this, ReturnToWarehouseActivity.class);
            intent.putExtra(
                    ReturnToWarehouseActivity.EXTRA_ASSIGNMENT_ID,
                    task.getAssignmentId()
            );
            startActivityForResult(intent, 9002);
        });
    }



    private void updateMainActionButton(String status) {
        int green = getResources().getColor(android.R.color.holo_green_dark);
        int gray = getResources().getColor(android.R.color.darker_gray);
        int blue = getResources().getColor(android.R.color.holo_blue_dark); // Màu cho nút Assign

        if (btnMainAction == null) return;
        switch (status) {
            case "ASSIGNED": // Trường hợp mới
                btnMainAction.setText("NHẬN NHIỆM VỤ");
                btnMainAction.setEnabled(true);
                btnMainAction.setVisibility(VISIBLE);
                btnFailAction.setVisibility(GONE); // Ẩn nút thất bại khi chưa nhận
                btnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue));
                break;
            case "IN_PROGRESS":
                btnMainAction.setText("HOÀN TẤT GIAO HÀNG");
                btnMainAction.setEnabled(true);
                btnMainAction.setVisibility(VISIBLE);
                btnFailAction.setVisibility(VISIBLE);
                btnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green));
                break;
            case "COMPLETED":
            case "FAILED":
            case "DELAYED":
                btnMainAction.setText("ĐÃ HOÀN TẤT");
                btnMainAction.setEnabled(false);
                btnFailAction.setVisibility(GONE);
                btnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gray));
                break;
            default:
                break;
        }
    }

    private void updateBottomButtonsByTaskStatus(DeliveryAssignment task) {
        if (task == null) return;

        String status = task.getStatus();

        boolean isReturnState =
                "FAILED".equalsIgnoreCase(status)
                        || "DELAYED".equalsIgnoreCase(status);

        if (isReturnState) {
            // 👉 ĐÈ NÚT TRẢ VỀ KHO
            btnReturnToWarehouse.setVisibility(VISIBLE);

            btnMainAction.setVisibility(GONE);
            btnFailAction.setVisibility(GONE);
        } else {
            // 👉 GIỮ NGUYÊN HÀNH VI CŨ
            btnReturnToWarehouse.setVisibility(GONE);

            // Logic hiển thị đã được xử lý trong updateMainActionButton
        }
    }


    /**
     * Disable actions when session is not IN_PROGRESS (e.g., CREATED).
     * Only allow: call, chat, and cancel/delay (btnFailAction).
     */
    private void updateButtonsBasedOnSessionStatus() {
        boolean isSessionActive = "IN_PROGRESS".equals(sessionStatus);
        boolean isAssignedTask = currentTask != null && "ASSIGNED".equals(currentTask.getStatus());

        if (!isSessionActive) {
            // Session not started - disable completion action UNLESS it is an ASSIGNED task waiting to be accepted
            if (btnMainAction != null) {
                if (isAssignedTask) {
                    // Allow accepting task even if session is not IN_PROGRESS (or not started yet)
                    btnMainAction.setEnabled(true);
                    // updateMainActionButton will handle text and color
                } else {
                    btnMainAction.setEnabled(false);
                    btnMainAction.setText("BẮT ĐẦU PHIÊN ĐỂ GIAO HÀNG");
                    btnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            getResources().getColor(android.R.color.darker_gray)));
                }
            }
            // btnFailAction is allowed (for DELAY/cancel)
            // btnCallReceiver is allowed
            // btnChatReceiver is allowed
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
                    String status = currentTask.getStatus();

                    if ("ASSIGNED".equals(status)) {
                        // Logic Nhận nhiệm vụ: Mở QrScanActivity để quét xác nhận
                        Intent intent = new Intent(TaskDetailActivity.this, QrScanActivity.class);
                        // Truyền các thông tin cần thiết để accept
                        intent.putExtra("SCAN_MODE", "ACCEPT_TASK");
                        intent.putExtra("PARCEL_CODE", currentTask.getParcelCode()); // ID để so khớp
                        intent.putExtra("ASSIGNMENT_ID", currentTask.getAssignmentId());
                        intent.putExtra("DRIVER_ID", sessionManager.getDriverId());

                        startActivityForResult(intent, REQUEST_CODE_ACCEPT_TASK);

                    } else if ("IN_PROGRESS".equals(status)) {
                        actionHandler.completeTaskWithProof(currentTask);
                    }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 9002 && resultCode == RESULT_OK) {
            // ReturnToWarehouseActivity completed successfully
            Toast.makeText(this, "Đã xác nhận trả hàng về kho!", Toast.LENGTH_SHORT).show();
            // Reload proofs to show RETURNED proof
            if (currentTask != null) {
                loadProofs(currentTask.getAssignmentId());
                updateBottomButtonsByTaskStatus(currentTask);

            }
            // Hide return button if proof exists
            updateReturnToWarehouseButton(currentTask);
        }
        else if (requestCode == REQUEST_CODE_ACCEPT_TASK && resultCode == RESULT_OK) {
            // Xử lý sau khi accept thành công từ QR Scan
            Toast.makeText(this, "Đã nhận nhiệm vụ thành công!", Toast.LENGTH_SHORT).show();

            // Cập nhật trạng thái local và UI
            currentTask.setStatus("IN_PROGRESS");
            displayData(currentTask);

            // Báo cho Activity cha (TaskFragment) biết để cập nhật list
            onStatusUpdated("IN_PROGRESS");
        }
        else if (actionHandler != null) {
            // CHUYỂN TIẾP KẾT QUẢ CHO HANDLER XỬ LÝ
            actionHandler.processProofResult(requestCode, resultCode, data);
        }
    }

//    /**
//     * Xử lý kết quả trả về từ HỎI QUYỀN
//     */
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == TaskActionHandler.REQUEST_CAMERA_PERMISSION) {
//            actionHandler.handlePermissionResult(requestCode, permissions, grantResults);
//        }
//    }


    private void loadProofs(String assignmentId) {
        if (assignmentId == null || assignmentId.isEmpty()) {
            cardProofs.setVisibility(GONE);
            return;
        }

        // Chỉ show loading text, KHÔNG show card vội
        cardProofs.setVisibility(GONE);
        tvProofsLoading.setVisibility(VISIBLE);
        tvProofsEmpty.setVisibility(GONE);
        recyclerProofs.setVisibility(GONE);

        sessionClient.getProofsByAssignment(assignmentId)
                .enqueue(new Callback<BaseResponse<List<DeliveryProof>>>() {
                    @Override
                    public void onResponse(
                            Call<BaseResponse<List<DeliveryProof>>> call,
                            Response<BaseResponse<List<DeliveryProof>>> response
                    ) {
                        tvProofsLoading.setVisibility(GONE);

                        if (!response.isSuccessful()
                                || response.body() == null
                                || response.body().getResult() == null
                                || response.body().getResult().isEmpty()) {

                            // ❌ Không có proof → ẩn toàn bộ card
                            currentProofs = new ArrayList<>();
                            cardProofs.setVisibility(GONE);
                            updateReturnToWarehouseButton(currentTask);
                            return;
                        }

                        // ✅ Có proof
                        currentProofs = response.body().getResult();
                        cardProofs.setVisibility(VISIBLE);
                        recyclerProofs.setVisibility(VISIBLE);
                        proofAdapter.setProofs(currentProofs);

                        updateReturnToWarehouseButton(currentTask);
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<List<DeliveryProof>>> call, Throwable t) {
                        tvProofsLoading.setVisibility(GONE);
                        cardProofs.setVisibility(GONE);
                    }
                });
    }


    @Override
    public void onStatusUpdated(String newStatus) {
        currentTask.setStatus(newStatus);
        updateBottomButtonsByTaskStatus(currentTask);

        updateMainActionButton(newStatus);

        // Pass updated task info back to TaskFragment
        Intent resultIntent = new Intent();
        resultIntent.putExtra("UPDATED_TASK", currentTask);
        resultIntent.putExtra("NEW_STATUS", newStatus);
        setResult(Activity.RESULT_OK, resultIntent);

        displayData(currentTask);

        Toast.makeText(this, "Đã cập nhật: " + newStatus, Toast.LENGTH_SHORT).show();

        // Reload proofs if status changed to COMPLETED
        if ("COMPLETED".equals(newStatus)) {
            loadProofs(currentTask.getAssignmentId());
        }

        finish();
    }
}