package com.ds.deliveryapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ds.deliveryapp.configs.RetrofitClient;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;

/**
 * Activity to display QR code for session ID (for transferring parcels)
 */
public class SessionQRDisplayActivity extends AppCompatActivity {

    private static final String TAG = "SessionQRDisplayActivity";
    public static final String EXTRA_SESSION_ID = "SESSION_ID";

    private ImageView ivQRCode;
    private TextView tvSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_qr_display);

        String sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        if (sessionId == null || sessionId.isEmpty()) {
            Toast.makeText(this, "Không có thông tin phiên.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ivQRCode = findViewById(R.id.ivQRCode);
        tvSessionId = findViewById(R.id.tvSessionId);

        tvSessionId.setText("Mã phiên: " + sessionId);

        // Generate QR code URL
        // Note: baseUrl already includes /api/v1/, so we only need /qr/generate
        Retrofit retrofit = RetrofitClient.getRetrofitInstance(this);
        String baseUrl = retrofit.baseUrl().toString();
        // Remove trailing slash if present, then add the endpoint
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        // URL encode the sessionId to handle any special characters
        String encodedSessionId = URLEncoder.encode(sessionId, StandardCharsets.UTF_8);
        String qrUrl = baseUrl + "/qr/generate?data=" + encodedSessionId;

        // Load QR code image using OkHttp (already in dependencies)
        loadQRCodeImage(qrUrl);
    }

    private void loadQRCodeImage(String imageUrl) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(imageUrl)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        InputStream inputStream = response.body().byteStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        
                        // Update UI on main thread
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (bitmap != null) {
                                ivQRCode.setImageBitmap(bitmap);
                            } else {
                                Log.e(TAG, "Failed to decode QR code image");
                                Toast.makeText(this, "Không thể tải mã QR.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Log.e(TAG, "Failed to load QR code: " + response.code());
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(this, "Không thể tải mã QR.", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading QR code image", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, "Lỗi tải mã QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
