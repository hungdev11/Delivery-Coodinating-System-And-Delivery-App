package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.req.AcceptTransferredParcelRequest;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.utils.SessionManager;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity to scan session QR code and accept transferred parcels
 * Flow: Scan QR -> Show list of ON_ROUTE parcels -> Select parcels to accept
 */
public class SessionQRScanActivity extends AppCompatActivity {

    private static final String TAG = "SessionQRScanActivity";

    private String driverId;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        driverId = sessionManager.getDriverId();
        if (driverId == null || driverId.isEmpty()) {
            Toast.makeText(this, "Không xác định được tài xế.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Start QR scanner
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Quét mã QR phiên của shipper chuyển đơn");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Hủy quét mã", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                handleScannedSessionId(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleScannedSessionId(String sessionId) {
        Log.d(TAG, "Scanned session ID: " + sessionId);
        
        // Fetch tasks from the scanned session
        fetchTasksFromSession(sessionId);
    }

    private void fetchTasksFromSession(String sourceSessionId) {
        SessionClient service = RetrofitClient.getRetrofitInstance(this).create(SessionClient.class);
        
        // Fetch all tasks from the source session (get a large page size to get all)
        Call<BaseResponse<PageResponse<DeliveryAssignment>>> call = service.getTasksBySessionId(
                sourceSessionId,
                0,
                100 // Get up to 100 tasks
        );

        call.enqueue(new Callback<BaseResponse<PageResponse<DeliveryAssignment>>>() {
            @Override
            public void onResponse(Call<BaseResponse<PageResponse<DeliveryAssignment>>> call, Response<BaseResponse<PageResponse<DeliveryAssignment>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<PageResponse<DeliveryAssignment>> baseResponse = response.body();
                    if (baseResponse.getResult() == null) {
                        String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Không thể tải danh sách đơn hàng";
                        Log.e(TAG, "Failed to fetch tasks: " + errorMsg);
                        Toast.makeText(SessionQRScanActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    PageResponse<DeliveryAssignment> pageResponse = baseResponse.getResult();
                    List<DeliveryAssignment> allTasks = pageResponse.content();
                    
                    // Filter only IN_PROGRESS tasks (ON_ROUTE parcels)
                    List<DeliveryAssignment> onRouteTasks = new ArrayList<>();
                    for (DeliveryAssignment task : allTasks) {
                        if ("IN_PROGRESS".equals(task.getStatus())) {
                            onRouteTasks.add(task);
                        }
                    }
                    
                    if (onRouteTasks.isEmpty()) {
                        Toast.makeText(SessionQRScanActivity.this, "Không có đơn hàng ON_ROUTE trong phiên này.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        // Show dialog to select parcels to accept
                        showParcelSelectionDialog(sourceSessionId, onRouteTasks);
                    }
                } else {
                    Log.e(TAG, "Failed to fetch tasks: " + response.code());
                    Toast.makeText(SessionQRScanActivity.this, "Không thể tải danh sách đơn hàng: " + response.code(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<PageResponse<DeliveryAssignment>>> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(SessionQRScanActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void showParcelSelectionDialog(String sourceSessionId, List<DeliveryAssignment> onRouteTasks) {
        // Create parcel code array for dialog
        String[] parcelCodes = new String[onRouteTasks.size()];
        for (int i = 0; i < onRouteTasks.size(); i++) {
            DeliveryAssignment task = onRouteTasks.get(i);
            String code = task.getParcelCode() != null ? task.getParcelCode() : task.getParcelId();
            String location = task.getDeliveryLocation() != null ? task.getDeliveryLocation() : "";
            parcelCodes[i] = code + (location.isEmpty() ? "" : " - " + location);
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn đơn hàng để nhận chuyển giao")
                .setItems(parcelCodes, (dialog, which) -> {
                    DeliveryAssignment selectedTask = onRouteTasks.get(which);
                    // Show confirmation dialog
                    showAcceptConfirmationDialog(sourceSessionId, selectedTask);
                })
                .setNegativeButton("Hủy", (dialog, which) -> finish())
                .show();
    }

    private void showAcceptConfirmationDialog(String sourceSessionId, DeliveryAssignment selectedTask) {
        String parcelCode = selectedTask.getParcelCode() != null ? selectedTask.getParcelCode() : selectedTask.getParcelId();
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận nhận đơn chuyển giao")
                .setMessage("Bạn có chắc chắn muốn nhận đơn hàng " + parcelCode + " từ shipper khác?")
                .setPositiveButton("Nhận", (dialog, which) -> {
                    acceptTransferredParcel(sourceSessionId, selectedTask.getParcelId());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void acceptTransferredParcel(String sourceSessionId, String parcelId) {
        SessionClient service = RetrofitClient.getRetrofitInstance(this).create(SessionClient.class);
        
        AcceptTransferredParcelRequest request = AcceptTransferredParcelRequest.builder()
                .sourceSessionId(sourceSessionId)
                .parcelId(parcelId)
                .build();

        Call<BaseResponse<DeliveryAssignment>> call = service.acceptTransferredParcel(driverId, request);

        call.enqueue(new Callback<BaseResponse<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<BaseResponse<DeliveryAssignment>> call, Response<BaseResponse<DeliveryAssignment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<DeliveryAssignment> baseResponse = response.body();
                    if (baseResponse.getResult() != null) {
                        Toast.makeText(SessionQRScanActivity.this, "Nhận đơn chuyển giao thành công.", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                    } else {
                        String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Không thể nhận đơn chuyển giao";
                        Log.e(TAG, "Error response: " + errorMsg);
                        Toast.makeText(SessionQRScanActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                    }
                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.code());
                    Toast.makeText(SessionQRScanActivity.this, "Không thể nhận đơn chuyển giao: " + response.code(), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                }
                finish();
            }

            @Override
            public void onFailure(Call<BaseResponse<DeliveryAssignment>> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(SessionQRScanActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }
}
