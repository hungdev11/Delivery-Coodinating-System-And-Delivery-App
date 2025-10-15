package com.ds.deliveryapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.ds.deliveryapp.clients.ParcelClient;
import com.ds.deliveryapp.configs.RetrofitClient;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bắt đầu quét QR ngay khi Activity được tạo
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Quét mã QR trên đơn hàng");
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
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ParcelClient service = retrofit.create(ParcelClient.class);
        Log.e(TAG, retrofit.baseUrl().toString());

        Call<Parcel> call = service.getParcelById(scannedCode);

        call.enqueue(new Callback<Parcel>() {
            @Override
            public void onResponse(Call<Parcel> call, Response<Parcel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Parcel parcel = response.body();
                    Toast.makeText(QrScanActivity.this, "Tải đơn hàng #" + parcel.getCode() + " thành công.", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(QrScanActivity.this, ParcelDetailActivity.class);
                    intent.putExtra("PARCEL_INFO", parcel);

                    // 🔥 SỬA LỖI: Gọi ParcelDetailActivity bằng ForResult
                    startActivityForResult(intent, PARCEL_DETAIL_REQUEST_CODE);

                    // KHÔNG GỌI finish() TẠI ĐÂY! Nó sẽ được gọi trong onActivityResult sau.
                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.code());
                    Toast.makeText(QrScanActivity.this, "Lỗi tải đơn hàng: " + response.code(), Toast.LENGTH_SHORT).show();
                    finish(); // Kết thúc nếu không tải được đơn hàng
                }
            }
            @Override
            public void onFailure(Call<Parcel> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(QrScanActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}