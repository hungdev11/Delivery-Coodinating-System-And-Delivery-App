package com.ds.deliveryapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.ds.deliveryapp.clients.ParcelClient;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.model.Parcel;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class QrScanActivity extends AppCompatActivity {

    private static final String TAG = "QrScanActivity";
    private static final int PARCEL_DETAIL_REQUEST_CODE = 1002;

    // Các biến cho chế độ ACCEPT_TASK
    private String scanMode;
    private String targetParcelCode;
    private String assignmentId;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Lấy dữ liệu từ Intent để biết đang ở chế độ nào
        Intent intent = getIntent();
        if (intent != null) {
            scanMode = intent.getStringExtra("SCAN_MODE");
            targetParcelCode = intent.getStringExtra("PARCEL_CODE");
            assignmentId = intent.getStringExtra("ASSIGNMENT_ID");
            driverId = intent.getStringExtra("DRIVER_ID");
        }

        // Bắt đầu quét QR ngay khi Activity được tạo
        IntentIntegrator integrator = new IntentIntegrator(this);
        String prompt = "Quét mã QR trên đơn hàng";

        if ("ACCEPT_TASK".equals(scanMode)) {
            prompt = "Quét mã kiện hàng để NHẬN NHIỆM VỤ\nMã yêu cầu: " + targetParcelCode;
        }

        integrator.setPrompt(prompt);
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            // Xử lý kết quả từ ZXing (quét mã QR)
            if (result.getContents() == null) {
                Toast.makeText(this, "Hủy quét mã", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                handleScannedCode(result.getContents());
            }
        } else if (requestCode == PARCEL_DETAIL_REQUEST_CODE) {
            setResult(resultCode); // Chuyển tiếp kết quả (RESULT_OK/CANCELED) về TaskFragment
            finish(); // Kết thúc QrScanActivity
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleScannedCode(String scannedCode) {
        if ("ACCEPT_TASK".equals(scanMode)) {
            // Logic nhận nhiệm vụ
            verifyAndAcceptTask(scannedCode);
        } else {
            // Logic cũ: Tra cứu thông tin kiện hàng
            //lookupParcelInfo(scannedCode);
        }
    }

    private void verifyAndAcceptTask(String scannedCode) {
        // 1. Kiểm tra mã
        // So sánh scannedCode với targetParcelCode (ID hoặc Code)
        if (scannedCode == null || targetParcelCode == null) {
            Toast.makeText(this, "Lỗi dữ liệu đơn hàng.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Chấp nhận nếu quét trúng Parcel Code hoặc Parcel ID (giả sử targetParcelCode lưu Code)
        if (!scannedCode.equals(targetParcelCode)) {
            // Thử so sánh lỏng lẻo hơn hoặc báo lỗi
            Toast.makeText(this, "Sai mã kiện hàng! \nMã quét: " + scannedCode + "\nMã yêu cầu: " + targetParcelCode, Toast.LENGTH_LONG).show();

            // Restart scan or finish? Here we finish to let user click button again, or could restart scan.
            finish();
            return;
        }

        // 2. Gọi API Accept Task
        Toast.makeText(this, "Mã khớp! Đang xác nhận nhận đơn...", Toast.LENGTH_SHORT).show();

        SessionClient sessionClient = RetrofitClient.getRetrofitInstance(this).create(SessionClient.class);
        Call<BaseResponse<DeliveryAssignment>> call = sessionClient.acceptTask(assignmentId, driverId);

        call.enqueue(new Callback<BaseResponse<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<BaseResponse<DeliveryAssignment>> call, Response<BaseResponse<DeliveryAssignment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Thành công
                    Toast.makeText(QrScanActivity.this, "Đã nhận nhiệm vụ thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Trả về OK cho TaskDetailActivity
                    finish();
                } else {
                    Toast.makeText(QrScanActivity.this, "Lỗi khi nhận nhiệm vụ: " + response.code(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<DeliveryAssignment>> call, Throwable t) {
                Toast.makeText(QrScanActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void lookupParcelInfo(String scannedCode) {
        // Logic cũ của bạn
        ParcelClient parcelClient = RetrofitClient.getRetrofitInstance(this).create(ParcelClient.class);
        Call<BaseResponse<Parcel>> call = parcelClient.getParcelById(scannedCode);

        Toast.makeText(this, "Đang tìm kiếm đơn hàng...", Toast.LENGTH_SHORT).show();

        call.enqueue(new Callback<BaseResponse<Parcel>>() {
            @Override
            public void onResponse(Call<BaseResponse<Parcel>> call, Response<BaseResponse<Parcel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Parcel> baseResponse = response.body();
                    if (baseResponse.getResult() == null) {
                        String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Không tìm thấy đơn hàng";
                        Toast.makeText(QrScanActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        finish(); // Finish if not found to avoid stuck
                        return;
                    }
                    Parcel parcel = baseResponse.getResult();
                    Toast.makeText(QrScanActivity.this, "Tải đơn hàng #" + parcel.getCode() + " thành công.", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(QrScanActivity.this, ParcelDetailActivity.class);
                    intent.putExtra("PARCEL_INFO", parcel);

                    startActivityForResult(intent, PARCEL_DETAIL_REQUEST_CODE);
                    // Finish will be called in onActivityResult
                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.code());
                    Toast.makeText(QrScanActivity.this, "Lỗi tải đơn hàng: " + response.code(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onFailure(Call<BaseResponse<Parcel>> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(QrScanActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}