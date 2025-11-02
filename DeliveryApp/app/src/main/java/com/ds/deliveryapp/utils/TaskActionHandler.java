package com.ds.deliveryapp.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.req.RouteInfo;
import com.ds.deliveryapp.clients.req.TaskFailRequest;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.model.IssueReason;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TaskActionHandler {

    private static final String TAG = "TASK_ACTION_HANDLER";

    private Activity activity;
    private Fragment fragment;
    private TaskUpdateListener listener;
    private final List<IssueReason> issueReasons;
    private String driverId;

    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final int REQUEST_CAMERA_PERMISSION = 101;

    private Uri photoURI;
    private DeliveryAssignment pendingAssignment;

    // === Constructor cho Fragment ===
    public TaskActionHandler(Fragment fragment, TaskUpdateListener listener) {
        this.fragment = fragment;
        this.activity = fragment.getActivity();
        this.listener = listener;
        this.issueReasons = initializeIssueReasons();

        // ✅ Lấy driverId từ AuthManager (hoặc SessionManager)
        SessionManager sessionManager = new SessionManager(activity.getApplicationContext());
        driverId = sessionManager.getDriverId();

        if (driverId == null || driverId.isEmpty()) {
            Toast.makeText(activity, "Không tìm thấy tài xế. Hãy đăng nhập lại.", Toast.LENGTH_LONG).show();
        }
    }

    // === Constructor cho Activity ===
    public TaskActionHandler(Activity activity, TaskUpdateListener listener) {
        this.activity = activity;
        this.fragment = null;
        this.listener = listener;
        this.issueReasons = initializeIssueReasons();

        SessionManager sessionManager = new SessionManager(activity.getApplicationContext());
        driverId = sessionManager.getDriverId();

        if (driverId == null || driverId.isEmpty()) {
            Toast.makeText(activity, "Không tìm thấy tài xế. Hãy đăng nhập lại.", Toast.LENGTH_LONG).show();
        }
    }

    public interface TaskUpdateListener {
        void onStatusUpdated(String newStatus);
    }

    private List<IssueReason> initializeIssueReasons() {
        List<IssueReason> reasons = new ArrayList<>();
        reasons.add(new IssueReason("Khách không liên lạc được", "CANNOT_CONTACT"));
        reasons.add(new IssueReason("Địa chỉ không tìm thấy", "PHANTOM_ADDRESS"));
        reasons.add(new IssueReason("Xe bị hỏng/sự cố", "ACCIDENT"));
        reasons.add(new IssueReason("Lý do khác", "CUSTOM"));
        return reasons;
    }

    // === FLOW HOÀN THÀNH ===
    public void startCompletionFlow(DeliveryAssignment assignment) {
        if (!"IN_PROGRESS".equals(assignment.getStatus())) {
            Toast.makeText(activity, "Trạng thái không hợp lệ: " + assignment.getStatus(), Toast.LENGTH_SHORT).show();
            return;
        }
        this.pendingAssignment = assignment;
        checkPermissionAndLaunchCamera();
    }

    private void checkPermissionAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (fragment != null) {
                fragment.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
        } else {
            launchCameraIntent();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void launchCameraIntent() {
        if (pendingAssignment == null) {
            Log.e(TAG, "pendingAssignment null.");
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Lỗi tạo file ảnh", ex);
                Toast.makeText(activity, "Không thể tạo file ảnh.", Toast.LENGTH_SHORT).show();
                return;
            }

            String authority = activity.getPackageName() + ".fileprovider";
            try {
                photoURI = FileProvider.getUriForFile(activity, authority, photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                if (fragment != null)
                    fragment.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                else
                    activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (Exception e) {
                Log.e(TAG, "FileProvider error", e);
            }
        } else {
            Toast.makeText(activity, "Không tìm thấy ứng dụng camera.", Toast.LENGTH_SHORT).show();
        }
    }

    public void handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (pendingAssignment == null || photoURI == null) {
                    Toast.makeText(activity, "Lỗi xử lý ảnh.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(activity, "Đang gửi yêu cầu hoàn thành...", Toast.LENGTH_LONG).show();
                sendCompletionRequest(pendingAssignment);
                listener.onStatusUpdated("COMPLETED");
            } else {
                Toast.makeText(activity, "Đã hủy chụp ảnh.", Toast.LENGTH_SHORT).show();
            }
            pendingAssignment = null;
            photoURI = null;
        }
    }

    // === FLOW THẤT BẠI ===
    public void startFailureFlow(DeliveryAssignment assignment) {
        showIssueDialog(assignment);
    }

    private void showIssueDialog(DeliveryAssignment assignment) {
        String[] reasonNames = issueReasons.stream().map(r -> r.display).toArray(String[]::new);
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
                .setNegativeButton("Hủy", (dialog, id) -> dialog.dismiss())
                .create().show();
    }

    private void showCustomReasonInputDialog(DeliveryAssignment assignment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Nhập lý do thất bại");
        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("Xác Nhận", (dialog, which) -> {
            String customReason = input.getText().toString().trim();
            if (customReason.isEmpty()) {
                Toast.makeText(activity, "Lý do không được để trống.", Toast.LENGTH_SHORT).show();
            } else {
                dispatchFailureEvent(assignment, null, customReason);
                listener.onStatusUpdated("FAILED");
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showConfirmationDialog(DeliveryAssignment assignment, IssueReason reason) {
        new AlertDialog.Builder(activity)
                .setTitle("Xác Nhận Thất Bại")
                .setMessage("Lý do: " + reason.display)
                .setPositiveButton("Xác Nhận", (dialog, id) -> {
                    dispatchFailureEvent(assignment, reason, null);
                    listener.onStatusUpdated("FAILED");
                })
                .setNegativeButton("Hủy", (dialog, id) -> dialog.dismiss())
                .create().show();
    }

    public void handlePermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                launchCameraIntent();
            else
                pendingAssignment = null;
        }
    }

    // === API CALLS ===
    private void sendCompletionRequest(DeliveryAssignment assignment) {
        if (assignment == null || driverId == null) {
            Log.e(TAG, "Thiếu assignment hoặc driverId");
            return;
        }
        Retrofit retrofit = RetrofitClient.getRetrofitInstance(activity.getApplicationContext());
        SessionClient service = retrofit.create(SessionClient.class);
        RouteInfo routeInfo = RouteInfo.builder().distanceM(1000).durationS(1000).waypoints("{}").build();
        Call<DeliveryAssignment> call = service.completeTask(driverId, assignment.getParcelId(), routeInfo);
        call.enqueue(new Callback<DeliveryAssignment>() {
            @Override
            public void onResponse(Call<DeliveryAssignment> call, Response<DeliveryAssignment> response) {
                if (response.isSuccessful())
                    Log.d(TAG, "Task COMPLETED event sent successfully.");
                else
                    Log.e(TAG, "Response unsuccessful: " + response.code());
            }

            @Override
            public void onFailure(Call<DeliveryAssignment> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
            }
        });
    }

    private void dispatchFailureEvent(DeliveryAssignment assignment, @Nullable IssueReason reason, @Nullable String customReason) {
        String finalReason = (reason != null) ? reason.getDisplay() : customReason;
        RouteInfo routeInfo = RouteInfo.builder().distanceM(1000).durationS(1000).waypoints("{}").build();
        TaskFailRequest body = new TaskFailRequest(finalReason, routeInfo);
        callFailApi(assignment, body);
    }

    private void callFailApi(DeliveryAssignment assignment, TaskFailRequest body) {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance(activity.getApplicationContext());
        SessionClient service = retrofit.create(SessionClient.class);
        Call<DeliveryAssignment> call = service.failTask(driverId, assignment.getParcelId(), body);
        call.enqueue(new Callback<DeliveryAssignment>() {
            @Override
            public void onResponse(Call<DeliveryAssignment> call, Response<DeliveryAssignment> response) {
                if (response.isSuccessful())
                    Log.d(TAG, "Task FAILED event sent successfully.");
                else
                    Log.e(TAG, "Response (Fail) unsuccessful: " + response.code());
            }

            @Override
            public void onFailure(Call<DeliveryAssignment> call, Throwable t) {
                Log.e(TAG, "Network error (Fail): " + t.getMessage());
            }
        });
    }
}
