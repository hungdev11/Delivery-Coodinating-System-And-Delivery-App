package com.ds.deliveryapp.utils;

import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.ds.deliveryapp.ProofActivity;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.model.IssueReason;

import java.util.ArrayList;
import java.util.List;

public class TaskActionHandler {

    private static final String TAG = "TASK_ACTION_HANDLER";
    public static final int REQUEST_CODE_PROOF = 9000;

    private Activity activity;
    private Fragment fragment;
    private TaskUpdateListener listener;
    private final List<IssueReason> issueReasons;
    private String driverId;
    private DeliveryAssignment pendingTask;

    // Biến để lưu lý do thất bại tạm thời
    private String pendingFailReason = null;

    public interface TaskUpdateListener {
        void onStatusUpdated(String newStatus);
    }

    public TaskActionHandler(Activity activity, TaskUpdateListener listener) {
        this.activity = activity;
        this.listener = listener;
        this.issueReasons = initializeIssueReasons();
        initCommon();
    }

    public TaskActionHandler(Fragment fragment, TaskUpdateListener listener) {
        this.fragment = fragment;
        this.activity = fragment.getActivity();
        this.listener = listener;
        this.issueReasons = initializeIssueReasons();
        initCommon();
    }

    private void initCommon() {
        SessionManager sessionManager = new SessionManager(activity.getApplicationContext());
        driverId = sessionManager.getDriverId();
    }

    // --- ENTRY POINTS ---
    public void completeTaskWithProof(DeliveryAssignment task) {
        if (!"IN_PROGRESS".equals(task.getStatus())) {
            Toast.makeText(activity, "Trạng thái đơn hàng không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }
        this.pendingTask = task;
        this.pendingFailReason = null; // Đảm bảo đây là luồng Success
        openProofActivity();
    }

    private void openProofActivity() {
        Intent intent = new Intent(activity, ProofActivity.class);
        // Ưu tiên dùng assignmentId nếu có
        if (pendingTask.getAssignmentId() != null && !pendingTask.getAssignmentId().isEmpty()) {
            intent.putExtra(ProofActivity.EXTRA_ASSIGNMENT_ID, pendingTask.getAssignmentId());
        } else {
            intent.putExtra(ProofActivity.EXTRA_PARCEL_ID, pendingTask.getParcelId());
            intent.putExtra(ProofActivity.EXTRA_DRIVER_ID, driverId);
        }

        // Truyền thêm flag để ProofActivity biết chế độ hoạt động
        boolean isFail = pendingFailReason != null;
        intent.putExtra(ProofActivity.EXTRA_IS_FAIL_REPORT, isFail);
        if (isFail) {
            intent.putExtra(ProofActivity.EXTRA_FAIL_REASON, pendingFailReason);
        }

        if (fragment != null) {
            fragment.startActivityForResult(intent, REQUEST_CODE_PROOF);
        } else {
            activity.startActivityForResult(intent, REQUEST_CODE_PROOF);
        }
    }

    // --- PROCESS RESULT ---
    public void processProofResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_PROOF && resultCode == Activity.RESULT_OK) {
            // ProofActivity đã xử lý xong toàn bộ (Upload + API Call)
            // Chỉ cần báo lại cho UI cập nhật
            if (listener != null) {
                listener.onStatusUpdated(pendingFailReason != null ? "FAILED" : "COMPLETED");
            }
            // Reset state
            pendingTask = null;
            pendingFailReason = null;
        }
    }

    // --- FAIL FLOW ---
    public void startFailureFlow(DeliveryAssignment assignment) {
        showIssueDialog(assignment);
    }

    private void showIssueDialog(DeliveryAssignment assignment) {
        String[] reasonNames = issueReasons.stream().map(IssueReason::getDisplay).toArray(String[]::new);
        new AlertDialog.Builder(activity)
                .setTitle("Báo Cáo Thất Bại")
                .setItems(reasonNames, (dialog, which) -> {
                    IssueReason selectedReason = issueReasons.get(which);
                    if ("CUSTOM".equals(selectedReason.getCode())) {
                        showCustomReasonInputDialog(assignment);
                    } else {
                        showConfirmationDialog(assignment, selectedReason);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showCustomReasonInputDialog(DeliveryAssignment assignment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Nhập lý do thất bại");
        final EditText input = new EditText(activity);
        builder.setView(input);
        builder.setPositiveButton("OK", (d, w) -> {
            String reason = input.getText().toString();
            if (reason.isEmpty()) {
                Toast.makeText(activity, "Vui lòng nhập lý do", Toast.LENGTH_SHORT).show();
                return;
            }
            // Chuyển sang chụp ảnh thay vì gọi API ngay
            dispatchFailureEvent(assignment, null, reason);
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showConfirmationDialog(DeliveryAssignment assignment, IssueReason reason) {
        new AlertDialog.Builder(activity).setTitle("Xác nhận").setMessage(reason.getDisplay())
                .setPositiveButton("OK", (d,w) -> {
                    // Chuyển sang chụp ảnh thay vì gọi API ngay
                    dispatchFailureEvent(assignment, reason, null);
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void dispatchFailureEvent(DeliveryAssignment assignment, IssueReason reason, String customReason) {
        this.pendingFailReason = (reason != null) ? reason.getDisplay() : customReason;
        this.pendingTask = assignment;

        // Mở màn hình chụp ảnh để xử lý tiếp
        openProofActivity();
    }

    private List<IssueReason> initializeIssueReasons() {
        List<IssueReason> reasons = new ArrayList<>();
        reasons.add(new IssueReason("Khách không liên lạc được", "CANNOT_CONTACT"));
        reasons.add(new IssueReason("Địa chỉ không tìm thấy", "PHANTOM_ADDRESS"));
        reasons.add(new IssueReason("Khách từ chối nhận", "REJECTED"));
        reasons.add(new IssueReason("Lý do khác", "CUSTOM"));
        return reasons;
    }
}