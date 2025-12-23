package com.ds.deliveryapp.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.ds.deliveryapp.ProofActivity;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.req.CompleteTaskRequest;
import com.ds.deliveryapp.clients.req.RouteInfo;
import com.ds.deliveryapp.clients.req.TaskFailRequest;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.model.IssueReason;
import com.ds.deliveryapp.service.CloudinaryService;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskActionHandler {

    private static final String TAG = "TASK_ACTION_HANDLER";
    public static final int REQUEST_CODE_PROOF = 9000;

    private Activity activity;
    private Fragment fragment;
    private TaskUpdateListener listener;
    private final List<IssueReason> issueReasons;
    private String driverId;
    private ProgressDialog progressDialog;
    private DeliveryAssignment pendingTask;

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
        this.progressDialog = new ProgressDialog(activity);
        this.progressDialog.setCancelable(false);
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
        openProofActivity();
    }

    private void openProofActivity() {
        Intent intent = new Intent(activity, ProofActivity.class);
        // Prefer assignmentId if available, otherwise fallback to parcelId + driverId
        if (pendingTask.getAssignmentId() != null && !pendingTask.getAssignmentId().isEmpty()) {
            intent.putExtra(ProofActivity.EXTRA_ASSIGNMENT_ID, pendingTask.getAssignmentId());
        } else {
            intent.putExtra(ProofActivity.EXTRA_PARCEL_ID, pendingTask.getParcelId());
            intent.putExtra(ProofActivity.EXTRA_DRIVER_ID, driverId);
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
            // ProofActivity now handles upload and submit internally
            // Just notify listener that task was completed
            if (listener != null) {
                listener.onStatusUpdated("COMPLETED");
            }
            pendingTask = null;
        }
    }

    private void uploadImagesToCloudinaryThenSubmit(List<String> imagePaths) {
        progressDialog.setMessage("Đang tải ảnh lên Cloudinary...");
        progressDialog.show();

        List<Uri> uris = new ArrayList<>();
        for (String path : imagePaths) {
            Log.e(TAG, "IMAGE PATH = " + path);
        }

        for (String path : imagePaths) {
            Uri uri = Uri.parse(path);
            uris.add(uri);
            Log.e(TAG, "USING URI = " + uri);
        }


        if (uris.isEmpty()) {
            progressDialog.dismiss();
            Toast.makeText(activity, "Không có ảnh hợp lệ để upload", Toast.LENGTH_SHORT).show();
            return;
        }

        CloudinaryService.getInstance()
                .uploadImages(activity, uris, new CloudinaryService.OnBatchUploadCallback() {

                    @Override
                    public void onComplete(List<String> successfulUrls) {
                        activity.runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Log.d(TAG, "Uploaded URLs: " + successfulUrls);

                            if (successfulUrls == null || successfulUrls.isEmpty()) {
                                Toast.makeText(activity, "Upload thất bại", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            submitCompletionRequestWithUrls(successfulUrls);
                        });
                    }

                    @Override
                    public void onError(String message) {
                        activity.runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }


    // --- BƯỚC 2: GỬI URL VỀ SERVER  ---
    private void submitCompletionRequestWithUrls(List<String> imageUrls) {
        progressDialog.setMessage("Đang đồng bộ dữ liệu...");
        // (Lưu ý: dialog đang show từ bước trước, chỉ update message)

        // 1. Tạo RouteInfo
        RouteInfo routeInfoObj = RouteInfo.builder()
                .distanceM(1000) // TODO: Lấy thực tế
                .durationS(1000)
                .waypoints("{}")
                .build();

        // 2. Tạo Request Body chứa RouteInfo và List URL
        // Sử dụng no-args constructor để tương thích với phiên bản mới (có thêm location)
        CompleteTaskRequest requestBody = new CompleteTaskRequest();
        requestBody.setRouteInfo(routeInfoObj);
        requestBody.setProofImageUrls(imageUrls);
        for (String url : imageUrls) {
            Log.e(TAG, "IMAGE URL = " + url);
        }
        Log.e(TAG, "REQUEST BODY = " + new Gson().toJson(requestBody));
        Log.e(TAG, "DRIVER ID = " + driverId);
        Log.e(TAG, "PARCEL ID = " + pendingTask.getParcelId());
        // 3. Gọi API (Dùng endpoint mới @Body)
        SessionClient service = RetrofitClient.getRetrofitInstance(activity).create(SessionClient.class);
        service.completeTaskWithUrls(driverId, pendingTask.getParcelId(), requestBody)
                .enqueue(new Callback<BaseResponse<DeliveryAssignment>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<DeliveryAssignment>> call, Response<BaseResponse<DeliveryAssignment>> response) {
                        progressDialog.dismiss();
                        Log.e(TAG, "RESPONSE = " + new Gson().toJson(response.body()));
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(activity, "Giao hàng thành công!", Toast.LENGTH_SHORT).show();
                            listener.onStatusUpdated("COMPLETED");
                            pendingTask = null;
                        } else {
                            Toast.makeText(activity, "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<DeliveryAssignment>> call, Throwable t) {
                        progressDialog.dismiss();
                        Toast.makeText(activity, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- FAIL FLOW (Giữ nguyên) ---
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
        builder.setPositiveButton("OK", (d, w) -> dispatchFailureEvent(assignment, null, input.getText().toString()));
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showConfirmationDialog(DeliveryAssignment assignment, IssueReason reason) {
        new AlertDialog.Builder(activity).setTitle("Xác nhận").setMessage(reason.getDisplay())
                .setPositiveButton("OK", (d,w) -> dispatchFailureEvent(assignment, reason, null))
                .setNegativeButton("Hủy", null).show();
    }

    private void dispatchFailureEvent(DeliveryAssignment assignment, IssueReason reason, String customReason) {
        String finalReason = (reason != null) ? reason.getDisplay() : customReason;
        RouteInfo routeInfo = RouteInfo.builder().distanceM(0).durationS(0).waypoints("{}").build();
        TaskFailRequest body = new TaskFailRequest(finalReason, routeInfo);

        SessionClient service = RetrofitClient.getRetrofitInstance(activity).create(SessionClient.class);
        service.failTask(driverId, assignment.getParcelId(), body).enqueue(new Callback<BaseResponse<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<BaseResponse<DeliveryAssignment>> call, Response<BaseResponse<DeliveryAssignment>> response) {
                if(response.isSuccessful()) listener.onStatusUpdated("FAILED");
            }
            @Override
            public void onFailure(Call<BaseResponse<DeliveryAssignment>> call, Throwable t) {}
        });
    }

    private List<IssueReason> initializeIssueReasons() {
        List<IssueReason> reasons = new ArrayList<>();
        reasons.add(new IssueReason("Khách không liên lạc được", "CANNOT_CONTACT"));
        reasons.add(new IssueReason("Địa chỉ không tìm thấy", "PHANTOM_ADDRESS"));
        reasons.add(new IssueReason("Sự cố gây hỏng hàng", "ACCIDENT"));
        //reasons.add(new IssueReason("Lý do khác", "CUSTOM"));
        return reasons;
    }
}
