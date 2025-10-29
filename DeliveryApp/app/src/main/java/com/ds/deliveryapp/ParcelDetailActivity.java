package com.ds.deliveryapp;

import static com.ds.deliveryapp.utils.FormaterUtil.formatCurrency;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ds.deliveryapp.clients.SessionClient;
// SỬA LỖI 1: Import DTO Request và Model Response
import com.ds.deliveryapp.clients.req.ScanParcelRequest;
import com.ds.deliveryapp.model.DeliveryAssignment;
// ---
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.Parcel;

import java.math.BigDecimal;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Màn hình Chi tiết Bưu kiện (khi quét QR).
 */
public class ParcelDetailActivity extends AppCompatActivity {

    private TextView tvParcelCode, tvStatus, tvDeliveryType, tvReceiveFrom,
            tvTargetDestination, tvDeliveryWindow, tvReceiverName,
            tvReceiverPhone, tvWeight, tvValue;
    private ImageButton btnClose;
    private Button btnAcceptTask;

    private String parcelId;
    private static final String DRIVER_ID = "0bbfa6a6-1c0b-4e4f-9e6e-11e36c142ea5";

    private static final String TAG = "ParcelDetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parcel_detail);

        initViews();
        loadDataFromIntent();
        setupEventListeners();
    }

    private void initViews() {
        tvParcelCode = findViewById(R.id.tvParcelCode);
        tvStatus = findViewById(R.id.tvStatus);
        tvDeliveryType = findViewById(R.id.tvDeliveryType);
        tvReceiveFrom = findViewById(R.id.tvReceiveFrom);
        tvTargetDestination = findViewById(R.id.tvTargetDestination);
        tvDeliveryWindow = findViewById(R.id.tvDeliveryWindow);
        tvReceiverName = findViewById(R.id.tvReceiverName);
        tvReceiverPhone = findViewById(R.id.tvReceiverPhone);
        tvWeight = findViewById(R.id.tvWeight);
        tvValue = findViewById(R.id.tvValue);
        btnClose = findViewById(R.id.btnClose);
        btnAcceptTask = findViewById(R.id.btnAcceptTask);
    }

    private void loadDataFromIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(this, "Lỗi: Không có dữ liệu đơn hàng.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Parcel parcel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel = intent.getSerializableExtra("PARCEL_INFO", Parcel.class);
        } else {
            parcel = (Parcel) intent.getSerializableExtra("PARCEL_INFO");
        }

        if (parcel == null) {
            Toast.makeText(this, "Không thể tải thông tin bưu kiện. Lỗi dữ liệu.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String code = parcel.getCode();
        parcelId = parcel.getId(); // Lấy ID
        String deliveryType = parcel.getDeliveryType() != null ? parcel.getDeliveryType().toString() : "N/A";
        String status = parcel.getStatus() != null ? parcel.getStatus().toString() : "N/A";
        String windowStart = parcel.getWindowStart() != null ? parcel.getWindowStart().toString() : "N/A";
        String windowEnd = parcel.getWindowEnd() != null ? parcel.getWindowEnd().toString() : "N/A";
        BigDecimal value = parcel.getValue();

        tvParcelCode.setText("Mã bưu kiện: #" + (code != null ? code : "N/A"));
        tvStatus.setText("Trạng thái: " + status);
        tvDeliveryType.setText("Loại giao hàng: " + deliveryType);
        tvReceiveFrom.setText("Từ: " + (parcel.getReceiveFrom() != null ? parcel.getReceiveFrom() : "N/A"));
        tvTargetDestination.setText("Đến: " + (parcel.getTargetDestination() != null ? parcel.getTargetDestination() : "N/A"));
        tvDeliveryWindow.setText("Thời gian giao: " + windowStart + " - " + windowEnd);
        tvReceiverName.setText("Người nhận (ID): " + (parcel.getReceiverId() != null ? parcel.getReceiverId() : "N/A"));
        tvReceiverPhone.setText("SĐT: " + (parcel.getReceiverPhoneNumber() != null ? parcel.getReceiverPhoneNumber() : "N/A"));
        tvWeight.setText("Khối lượng: " + String.format(Locale.getDefault(), "%.2f kg", parcel.getWeight()));
        tvValue.setText("Giá trị: " + formatCurrency(value));
        Log.d(TAG, "Parcel details loaded for code: " + code);

        if (parcel.getStatus() != null && !parcel.getStatus().toString().equals("IN_WAREHOUSE")) {
            btnAcceptTask.setText("KHÔNG THỂ NHẬN NHIỆM VỤ");
            btnAcceptTask.setEnabled(false);
        }
    }


    private void setupEventListeners() {
        btnClose.setOnClickListener(v -> finish());

        btnAcceptTask.setOnClickListener(v -> {
            if (parcelId == null || parcelId.isEmpty()) {
                Toast.makeText(this, "Lỗi: Không có Parcel ID.", Toast.LENGTH_SHORT).show();
                return;
            }
            btnAcceptTask.setEnabled(false);
            Toast.makeText(this, "Đang xử lý nhận nhiệm vụ...", Toast.LENGTH_SHORT).show();

            // SỬA LỖI 1: Gọi hàm mới
            handleAcceptTask(parcelId);
        });
    }

    private void handleAcceptTask(String parcelIdToAccept) {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance(this);
        SessionClient service = retrofit.create(SessionClient.class);

        // 1. Tạo Request DTO
        ScanParcelRequest requestBody = new ScanParcelRequest(parcelIdToAccept);

        // 2. Gọi API đúng (từ SessionController)
        Call<DeliveryAssignment> call = service.acceptParcelToSession(DRIVER_ID, requestBody);

        // 3. Sửa kiểu trả về (Callback<DeliveryAssignment>)
        call.enqueue(new Callback<DeliveryAssignment>() {
            @Override
            public void onResponse(Call<DeliveryAssignment> call, Response<DeliveryAssignment> response) {

                if (response.isSuccessful() && response.body() != null) {
                    // Thành công, server trả về task (Assignment) vừa tạo
                    Toast.makeText(ParcelDetailActivity.this, "Nhận đơn thành công. Nhiệm vụ đã được thêm.", Toast.LENGTH_LONG).show();

                    setResult(RESULT_OK); // Báo cho TaskFragment tải lại
                    finish();

                } else {
                    // Thất bại (ví dụ: 4xx, 5xx)
                    // (Đơn này đã được nhận, hoặc server lỗi)
                    Log.e(TAG, "Response unsuccessful: " + response.code() + ". Message: " + response.message());
                    Toast.makeText(ParcelDetailActivity.this, "Lỗi nhận nhiệm vụ: " + response.code(), Toast.LENGTH_LONG).show();

                    setResult(RESULT_CANCELED);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<DeliveryAssignment> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(ParcelDetailActivity.this, "Lỗi kết nối mạng khi nhận nhiệm vụ.", Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }
}
