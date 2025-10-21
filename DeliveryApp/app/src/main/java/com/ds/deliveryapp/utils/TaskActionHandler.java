package com.ds.deliveryapp.utils;

import android.app.Activity;
import android.content.Intent;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.req.RouteInfo;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.model.IssueReason;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TaskActionHandler {

    private final Activity activity;
    private final List<IssueReason> issueReasons;

    public static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final String TAG = "TASK_ACTION_HANDLER";
    public static final int REQUEST_QR_SCAN = 2; // Giả định cho luồng quét QR/Camera

    // Interface callback để thông báo cho Activity/Fragment cha khi trạng thái thay đổi
    public interface TaskUpdateListener {
        void onStatusUpdated(String newStatus);
    }

    private TaskUpdateListener listener;

    public TaskActionHandler(Activity activity, TaskUpdateListener listener) {
        this.activity = activity;
        this.listener = listener;
        this.issueReasons = initializeIssueReasons();
    }

    // --- KHỞI TẠO LÝ DO THẤT BẠI ---
    private List<IssueReason> initializeIssueReasons() {
        List<IssueReason> reasons = new ArrayList<>();
        reasons.add(new IssueReason("Khách hàng không liên lạc được/ Địa chỉ không tìm thấy", "CAN_NOT_DELIVERY"));
        reasons.add(new IssueReason("Xe bị hỏng/sự cố giao thông", "ACCIDENT"));
        reasons.add(new IssueReason("Khách hàng từ chối nhận hàng", "CUSTOMER_REFUSED"));
        reasons.add(new IssueReason("Hết phiên giao", "SESSION_TIMEOUT"));
        return reasons;
    }

    // --- HÀNH ĐỘNG 1: HOÀN TẤT (Cần Chụp Ảnh + Quét QR) ---
    public void startCompletionFlow(DeliveryAssignment assignment) {
        if (assignment.getStatus().equals("PROCESSING")) {
            dispatchConfirmationFlow(); // Bắt đầu flow chụp/quét
        } else {
            Toast.makeText(activity, "Không thể hoàn thành, trạng thái hiện tại là: " + assignment.getStatus(), Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchConfirmationFlow() {
        // 💡 Bước 1: Yêu cầu chụp ảnh (hoặc mở Activity tùy chỉnh cho Camera+QR)
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(activity, "Không tìm thấy ứng dụng camera.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- HÀNH ĐỘNG 2: BÁO CÁO THẤT BẠI ---
    public void startFailureFlow(DeliveryAssignment assignment) {
        showIssueDialog(assignment);
    }

    private void showIssueDialog(DeliveryAssignment assignment) {
        String[] reasonNames = issueReasons.stream()
                .map(r -> r.display)
                .toArray(String[]::new);

        // Dùng AlertDialog của Activity (hoặc AppCompatActivity)
        new AlertDialog.Builder(activity)
                .setTitle("Báo Cáo Thất Bại")
                .setItems(reasonNames, (dialog, which) -> {
                    IssueReason selectedReason = issueReasons.get(which);
                    showConfirmationDialog(assignment, selectedReason);
                })
                .setNegativeButton("Hủy", (dialog, id) -> dialog.dismiss())
                .create().show();
    }

    private void showConfirmationDialog(DeliveryAssignment assignment, IssueReason reason) {
        new AlertDialog.Builder(activity)
                .setTitle("Xác Nhận Thất Bại")
                .setMessage("Xác nhận lý do THẤT BẠI:\n\"" + reason.display + "\"\ncho đơn hàng " + assignment.getParcelCode() + "?")
                .setPositiveButton("Xác Nhận", (dialog, id) -> {
                    // Gửi event lên server và cập nhật trạng thái UI/Model
                    sendFailureEvent(assignment, reason);
                    listener.onStatusUpdated("FAILED"); // 💡 Thông báo cho Activity/Fragment
                    dialog.dismiss();
                })
                .setNegativeButton("Quay Lại", (dialog, id) -> dialog.dismiss())
                .show();
    }

    // --- XỬ LÝ KẾT QUẢ CAMERA ---
    public void handleActivityResult(int requestCode, int resultCode, @Nullable Intent data, DeliveryAssignment assignment) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            // TODO: Xử lý lưu ảnh và mở màn hình quét QR (nếu cần)

            // Giả định: Đã chụp ảnh và quét QR thành công
            sendCompletionRequest(assignment); // Gửi yêu cầu hoàn thành
            listener.onStatusUpdated("COMPLETED"); // 💡 Thông báo cho Activity/Fragment
            Toast.makeText(activity, "Đã chụp ảnh và gửi yêu cầu hoàn thành.", Toast.LENGTH_LONG).show();
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(activity, "Đã hủy chụp ảnh. Không thể hoàn thành đơn hàng.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendCompletionRequest(DeliveryAssignment assignment) {
        //open camera scan parcel qr and compare to current task : true update status, false error
        boolean flag = true;
        if (!flag) {
            Log.e(TAG, "NOT APPROPRIATE PARCEL AND CURRENT TASK");
            return;
        }
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();

        SessionClient service = retrofit.create(SessionClient.class);

        // get Route Info
        RouteInfo routeInfo = RouteInfo.builder().distanceM(1000).durationS(1000).waypoints("{}").build();

        Call<DeliveryAssignment> call = service.completeTask(assignment.getParcelId(), assignment.getDeliveryManAssignedId(), routeInfo);

        call.enqueue(new Callback<DeliveryAssignment>() {
            @Override
            public void onResponse(Call<DeliveryAssignment> call, Response<DeliveryAssignment> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Task COMPLETED event sent successfully.");
                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.code() + ". Message: " + response.message());
                    // Xử lý lỗi API
                }
            }

            @Override
            public void onFailure(Call<DeliveryAssignment> call, Throwable t) {
                Log.e(TAG, "Network error on failure: " + t.getMessage());
            }
        });
    }

    private void sendFailureEvent(DeliveryAssignment assignment, IssueReason reason) {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();

        SessionClient service = retrofit.create(SessionClient.class);

        // get Route Info
        RouteInfo routeInfo = RouteInfo.builder().distanceM(1000).durationS(1000).waypoints("{}").build();

        boolean flag = "CUSTOMER_REFUSED".equals(reason.getCode());

        Call<DeliveryAssignment> call = service.failTask(assignment.getParcelId(), assignment.getDeliveryManAssignedId(), flag, reason.getDisplay(), routeInfo);

        call.enqueue(new Callback<DeliveryAssignment>() {
            @Override
            public void onResponse(Call<DeliveryAssignment> call, Response<DeliveryAssignment> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Task FAILED event sent successfully.");
                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.code() + ". Message: " + response.message());
                    // Xử lý lỗi API
                }
            }

            @Override
            public void onFailure(Call<DeliveryAssignment> call, Throwable t) {
                Log.e(TAG, "Network error on failure: " + t.getMessage());
            }
        });
    }
}