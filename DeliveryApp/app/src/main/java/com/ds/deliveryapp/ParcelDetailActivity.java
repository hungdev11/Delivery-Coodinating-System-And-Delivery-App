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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ds.deliveryapp.auth.AuthManager;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.req.ScanParcelRequest;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.model.Parcel;
import com.ds.deliveryapp.utils.SessionManager;

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
    private AuthManager mAuthManager;
    private SessionManager mSessionManager;
    private String parcelId;
    private String mJwtToken;
    private String driverId;

    private static final String TAG = "ParcelDetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parcel_detail);

        initViews();
        mAuthManager = new AuthManager(this);
        mSessionManager = new SessionManager(this);
        mJwtToken = mAuthManager.getAccessToken();

        if (mJwtToken == null || mJwtToken.isEmpty()) {
            Toast.makeText(this, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        driverId = mSessionManager.getDriverId();
        if (driverId == null || driverId.isEmpty()) {
            Toast.makeText(this, "Không xác định được tài xế. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Driver ID (from SessionManager): " + driverId);

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
            Toast.makeText(this, "Không có dữ liệu bưu kiện.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Parcel parcel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? intent.getSerializableExtra("PARCEL_INFO", Parcel.class)
                : (Parcel) intent.getSerializableExtra("PARCEL_INFO");

        if (parcel == null) {
            Toast.makeText(this, "Không thể tải thông tin bưu kiện.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        parcelId = parcel.getId();
        tvParcelCode.setText("Mã bưu kiện: #" + parcel.getCode());
        tvStatus.setText("Trạng thái: " + parcel.getStatus());
        tvDeliveryType.setText("Loại giao hàng: " + parcel.getDeliveryType());
        tvReceiveFrom.setText("Từ: " + parcel.getReceiveFrom());
        tvTargetDestination.setText("Đến: " + parcel.getTargetDestination());
        tvDeliveryWindow.setText("Thời gian giao: " + parcel.getWindowStart() + " - " + parcel.getWindowEnd());
        tvReceiverName.setText("Người nhận (ID): " + parcel.getReceiverId());
        tvReceiverPhone.setText("SĐT: " + parcel.getReceiverPhoneNumber());
        tvWeight.setText(String.format(Locale.getDefault(), "%.2f kg", parcel.getWeight()));
        tvValue.setText("Giá trị: " + formatCurrency(parcel.getValue()));

        if (!"IN_WAREHOUSE".equals(parcel.getStatus().toString())) {
            btnAcceptTask.setText("KHÔNG THỂ NHẬN NHIỆM VỤ");
            btnAcceptTask.setEnabled(false);
        }
    }

    private void setupEventListeners() {
        btnClose.setOnClickListener(v -> finish());
        btnAcceptTask.setOnClickListener(v -> {
            if (driverId == null) {
                Toast.makeText(this, "Không xác định được tài xế. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (parcelId == null || parcelId.isEmpty()) {
                Toast.makeText(this, "Thiếu Parcel ID.", Toast.LENGTH_SHORT).show();
                return;
            }
            btnAcceptTask.setEnabled(false);
            handleAcceptTask(parcelId);
        });
    }

    private void handleAcceptTask(String parcelIdToAccept) {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance(this);
        SessionClient service = retrofit.create(SessionClient.class);

        ScanParcelRequest requestBody = new ScanParcelRequest(parcelIdToAccept);
        Call<DeliveryAssignment> call = service.acceptParcelToSession(driverId, requestBody);

        call.enqueue(new Callback<DeliveryAssignment>() {
            @Override
            public void onResponse(Call<DeliveryAssignment> call, Response<DeliveryAssignment> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(ParcelDetailActivity.this, "Nhận đơn thành công.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                } else {
                    Log.e(TAG, "Lỗi khi nhận đơn: " + response.code());
                    Toast.makeText(ParcelDetailActivity.this, "Không thể nhận nhiệm vụ.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                }
                finish();
            }

            @Override
            public void onFailure(Call<DeliveryAssignment> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(ParcelDetailActivity.this, "Lỗi mạng khi nhận nhiệm vụ.", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }
}
