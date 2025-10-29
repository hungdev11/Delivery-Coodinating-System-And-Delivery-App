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

    // ... (Giữ nguyên các biến thành viên) ...
    private Activity activity;
    private Fragment fragment;
    private TaskUpdateListener listener;
    private final List<IssueReason> issueReasons;
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final String TAG = "TASK_ACTION_HANDLER";
    private static final String DRIVER_ID = "0bbfa6a6-1c0b-4e4f-9e6e-11e36c142ea5";

    // 💡 SỬA LỖI: BẬT LẠI photoURI.
    // Chúng ta CẦN nó để lưu trữ đường dẫn file ảnh khi dùng EXTRA_OUTPUT
    private Uri photoURI;
    private DeliveryAssignment pendingAssignment;

    // Constructor cho Fragment (Dùng cho Dialog)
    public TaskActionHandler(Fragment fragment, TaskUpdateListener listener) {
        this.fragment = fragment;
        this.activity = fragment.getActivity();
        this.listener = listener;
        this.issueReasons = initializeIssueReasons();
    }

    // Constructor cho Activity (Dùng cho TaskDetailActivity)
    public TaskActionHandler(Activity activity, TaskUpdateListener listener) {
        this.activity = activity;
        this.fragment = null;
        this.listener = listener;
        this.issueReasons = initializeIssueReasons();
    }

    public interface TaskUpdateListener {
        void onStatusUpdated(String newStatus);
    }

    private List<IssueReason> initializeIssueReasons() {
        // ... (Giữ nguyên) ...
        List<IssueReason> reasons = new ArrayList<>();
        reasons.add(new IssueReason("Khách không liên lạc được", "CANNOT_CONTACT"));
        reasons.add(new IssueReason("Địa chỉ không tìm thấy", "PHANTOM ADDRESS"));
        reasons.add(new IssueReason("Xe bị hỏng/sự cố", "ACCIDENT"));
        reasons.add(new IssueReason("Lý do khác", "CUSTOM"));
        return reasons;
    }

    public void startCompletionFlow(DeliveryAssignment assignment) {
        // ... (Giữ nguyên) ...
        if (!assignment.getStatus().equals("IN_PROGRESS")) {
            Toast.makeText(activity, "Trạng thái không hợp lệ: " + assignment.getStatus(), Toast.LENGTH_SHORT).show();
            return;
        }
        this.pendingAssignment = assignment;
        checkPermissionAndLaunchCamera();
    }

    private void checkPermissionAndLaunchCamera() {
        // ... (Giữ nguyên) ...
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (fragment != null) {
                // 💡 SỬA: Gọi requestPermissions trên Fragment
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

        // 💡 SỬA: Dùng getExternalFilesDir(Environment.DIRECTORY_PICTURES) cho rõ ràng
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        // 💡 SỬA: KHÔNG lưu Uri.fromFile(image).
        // Chúng ta sẽ lấy Uri từ FileProvider trong launchCameraIntent.
        return image;
    }

    private void launchCameraIntent() {
        if (pendingAssignment == null) {
            Log.e(TAG, "Lỗi: pendingAssignment là null khi gọi camera.");
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {

            // --- 💡 SỬA LỖI: BẬT LẠI LOGIC FILEPROVIDER ---
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Lỗi tạo file ảnh", ex);
                Toast.makeText(activity, "Lỗi: Không thể tạo file ảnh.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                // ⚠️ QUAN TRỌNG: Đảm bảo bạn đã khai báo FileProvider trong AndroidManifest.xml
                // và tạo file provider_paths.xml (xem ghi chú ở cuối)
                String authority = activity.getPackageName() + ".fileprovider";
                try {
                    photoURI = FileProvider.getUriForFile(activity, authority, photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                    Log.d(TAG, "launchCameraIntent: Calling startActivityForResult VỚI EXTRA_OUTPUT. Uri: " + photoURI);
                    if (fragment != null) {
                        fragment.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    } else {
                        activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }

                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Lỗi FileProvider. Bạn đã khai báo đúng 'authority' trong Manifest và file_paths.xml chưa?", e);
                    Toast.makeText(activity, "Lỗi cấu hình FileProvider.", Toast.LENGTH_LONG).show();
                    // Reset
                    photoURI = null;
                    pendingAssignment = null;
                }
            }
            // --- KẾT THÚC SỬA LỖI ---

        } else {
            Toast.makeText(activity, "Lỗi: Không tìm thấy ứng dụng camera.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Không tìm thấy ứng dụng camera. (Đã thêm <queries> trong Manifest chưa?)");
        }
    }

    // 💡 SỬA LỖI: THAY ĐỔI HOÀN TOÀN LOGIC XỬ LÝ KẾT QUẢ
    public void handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "handleActivityResult - Request Code: " + requestCode + ", Result Code: " + resultCode);

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                // 1. Kiểm tra xem pendingAssignment và photoURI có tồn tại không
                if (pendingAssignment == null || photoURI == null) {
                    Log.e(TAG, "Lỗi: Đã chụp ảnh xong nhưng pendingAssignment hoặc photoURI là null.");
                    Toast.makeText(activity, "Đã xảy ra lỗi khi lưu ảnh.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 2. Vì đã dùng EXTRA_OUTPUT, 'data' Intent sẽ (hoặc có thể) là NULL.
                // Ảnh đầy đủ nằm ở 'photoURI'.
                Log.d(TAG, "Ảnh đã được chụp và lưu tại: " + photoURI.toString());

                // 3. TODO: UPLOAD ẢNH LÊN SERVER
                // Đây là nơi bạn sẽ gọi service (ví dụ: Retrofit + Multipart)
                // để tải file ảnh từ 'photoURI' lên máy chủ làm bằng chứng giao hàng.

                // 4. (Tạm thời) Giả định upload thành công và tiếp tục luồng
                Toast.makeText(activity, "Đã chụp ảnh. Đang gửi yêu cầu hoàn thành...", Toast.LENGTH_LONG).show();
                sendCompletionRequest(pendingAssignment);
                listener.onStatusUpdated("COMPLETED");

            } else if (resultCode == Activity.RESULT_CANCELED) {
                // Người dùng hủy chụp ảnh
                Toast.makeText(activity, "Đã hủy chụp ảnh.", Toast.LENGTH_SHORT).show();
            }

            // 5. Dọn dẹp
            // Dù thành công hay thất bại, hãy xóa các biến tạm
            this.pendingAssignment = null;
            this.photoURI = null;
        }
    }

    // --- (Các hàm logic failure giữ nguyên) ---
    public void startFailureFlow(DeliveryAssignment assignment) {
        // ... (Giữ nguyên) ...
        showIssueDialog(assignment);
    }
    private void showIssueDialog(DeliveryAssignment assignment) {
        // ... (Giữ nguyên) ...
        String[] reasonNames = issueReasons.stream()
                .map(r -> r.display)
                .toArray(String[]::new);
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
        // ... (Giữ nguyên) ...
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Nhập lý do thất bại tùy chỉnh");
        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Ví dụ: Hàng cấm, không đúng kích thước...");
        builder.setView(input);
        builder.setPositiveButton("Xác Nhận", (dialog, which) -> {
            String customReason = input.getText().toString().trim();
            if (customReason.isEmpty()) {
                Toast.makeText(activity, "Lý do không được để trống.", Toast.LENGTH_SHORT).show();
            } else {
                dispatchFailureEvent(assignment, null, customReason);
                listener.onStatusUpdated("FAILED");
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void showConfirmationDialog(DeliveryAssignment assignment, IssueReason reason) {
        // ... (Giữ nguyên) ...
        new AlertDialog.Builder(activity)
                .setTitle("Xác Nhận Thất Bại")
                .setMessage("Xác nhận lý do THẤT BẠI:\n\"" + reason.display + "\"\ncho đơn hàng " + assignment.getParcelCode() + "?")
                .setPositiveButton("Xác Nhận", (dialog, id) -> {
                    dispatchFailureEvent(assignment, reason, null);
                    listener.onStatusUpdated("FAILED");
                    dialog.dismiss();
                })
                .setNegativeButton("Quay Lại", (dialog, id) -> dialog.dismiss())
                .create().show();
    }

    // --- (Hàm handlePermissionResult giữ nguyên) ---
    public void handlePermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(activity, "Đã cấp quyền Camera. Mở camera...", Toast.LENGTH_SHORT).show();
                launchCameraIntent();
            } else {
                Toast.makeText(activity, "Bạn phải cấp quyền camera để hoàn thành đơn hàng.", Toast.LENGTH_LONG).show();
                // 💡 THÊM: Reset pendingAssignment nếu bị từ chối quyền
                this.pendingAssignment = null;
            }
        }
    }

    // --- (Toàn bộ logic gọi API: sendCompletionRequest, dispatchFailureEvent... giữ nguyên) ---
    private void sendCompletionRequest(DeliveryAssignment assignment) {
        // ... (Giữ nguyên) ...
        if (assignment == null) {
            Log.e(TAG, "Lỗi: Assignment là null khi gửi request.");
            return;
        }
        boolean flag = true;
        if (!flag) {
            Log.e(TAG, "NOT APPROPRIATE PARCEL AND CURRENT TASK");
            return;
        }
        Retrofit retrofit = RetrofitClient.getRetrofitInstance(activity.getApplicationContext());
        SessionClient service = retrofit.create(SessionClient.class);
        RouteInfo routeInfo = RouteInfo.builder().distanceM(1000).durationS(1000).waypoints("{}").build();
        Call<DeliveryAssignment> call = service.completeTask(DRIVER_ID, assignment.getParcelId(), routeInfo);
        call.enqueue(new Callback<DeliveryAssignment>() {
            @Override
            public void onResponse(Call<DeliveryAssignment> call, Response<DeliveryAssignment> response) {
                if (response.isSuccessful()) { Log.d(TAG, "Task COMPLETED event sent successfully."); }
                else { Log.e(TAG, "Response unsuccessful: " + response.code()); }
            }
            @Override
            public void onFailure(Call<DeliveryAssignment> call, Throwable t) {
                Log.e(TAG, "Network error on failure: " + t.getMessage());
            }
        });
    }
    private void dispatchFailureEvent(DeliveryAssignment assignment, @Nullable IssueReason reason, @Nullable String customReason) {
        // ... (Giữ nguyên) ...
        String finalReason = (reason != null) ? reason.getDisplay() : customReason;
        String apiValue = (reason != null) ? reason.getCode() : "CUSTOM";
        RouteInfo routeInfo = RouteInfo.builder().distanceM(1000).durationS(1000).waypoints("{}").build();
        TaskFailRequest requestBody = new TaskFailRequest(finalReason, routeInfo);
        switch (apiValue) {
            case "CANNOT_CONTACT":
            case "ACCIDENT":
            case "PHANTOM_ADDRESS":
            case "CUSTOM":
            default:
                callFailApi(assignment, requestBody);
                break;
        }
    }
    private void callFailApi(DeliveryAssignment assignment, TaskFailRequest requestBody) {
        // ... (Giữ nguyên) ...
        Retrofit retrofit = RetrofitClient.getRetrofitInstance(activity.getApplicationContext());
        SessionClient service = retrofit.create(SessionClient.class);
        Call<DeliveryAssignment> call = service.failTask(DRIVER_ID, assignment.getParcelId(), requestBody);
        call.enqueue(new Callback<DeliveryAssignment>() {
            @Override
            public void onResponse(Call<DeliveryAssignment> call, Response<DeliveryAssignment> response) {
                if (response.isSuccessful()) { Log.d(TAG, "Task FAILED event sent successfully."); }
                else { Log.e(TAG, "Response (Fail) unsuccessful: " + response.code()); }
            }
            @Override
            public void onFailure(Call<DeliveryAssignment> call, Throwable t) {
                Log.e(TAG, "Network error (Fail): " + t.getMessage());
            }
        });
    }
    private void callRefuseApi(DeliveryAssignment assignment, TaskFailRequest requestBody) {
        // ... (Giữ nguyên) ...
        Retrofit retrofit = RetrofitClient.getRetrofitInstance(activity.getApplicationContext());
        SessionClient service = retrofit.create(SessionClient.class);
        Call<DeliveryAssignment> call = service.refuseTask(DRIVER_ID, assignment.getParcelId(), requestBody);
        call.enqueue(new Callback<DeliveryAssignment>() {
            @Override
            public void onResponse(Call<DeliveryAssignment> call, Response<DeliveryAssignment> response) {
                if (response.isSuccessful()) { Log.d(TAG, "Task REFUSED event sent successfully."); }
                else { Log.e(TAG, "Response (Refuse) unsuccessful: " + response.code()); }
            }
            @Override
            public void onFailure(Call<DeliveryAssignment> call, Throwable t) {
                Log.e(TAG, "Network error (Refuse): " + t.getMessage());
            }
        });
    }
}