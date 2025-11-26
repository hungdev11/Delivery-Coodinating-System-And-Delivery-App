package com.ds.deliveryapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.res.DeliverySession;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dashboard fragment hiển thị thời gian hiện tại và nút bắt đầu session.
 * Được hiển thị khi shipper chưa có session CREATED hoặc IN_PROGRESS.
 */
public class SessionDashboardFragment extends Fragment {

    private static final String TAG = "SessionDashboardFragment";
    
    private TextView tvCurrentTime;
    private TextView tvCurrentDate;
    private Button btnStartSession;
    private TextView tvSessionStatus;
    
    private Handler timeHandler;
    private Runnable timeRunnable;
    private SessionManager sessionManager;
    private String driverId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_session_dashboard, container, false);

        sessionManager = new SessionManager(requireContext());
        driverId = sessionManager.getDriverId();

        tvCurrentTime = view.findViewById(R.id.tv_current_time);
        tvCurrentDate = view.findViewById(R.id.tv_current_date);
        btnStartSession = view.findViewById(R.id.btn_start_session);
        tvSessionStatus = view.findViewById(R.id.tv_session_status);

        btnStartSession.setOnClickListener(v -> createSessionPrepared());

        startTimeUpdates();
        checkExistingSession();

        return view;
    }

    private void startTimeUpdates() {
        timeHandler = new Handler(Looper.getMainLooper());
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                updateTime();
                timeHandler.postDelayed(this, 1000); // Update every second
            }
        };
        timeHandler.post(timeRunnable);
    }

    private void updateTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
        
        Date now = new Date();
        tvCurrentTime.setText(timeFormat.format(now));
        tvCurrentDate.setText(dateFormat.format(now));
    }

    private void checkExistingSession() {
        // Check if there's an existing CREATED or IN_PROGRESS session
        // This will be handled by TaskFragment, but we can show status here if needed
    }

    private void createSessionPrepared() {
        if (driverId == null || driverId.isEmpty()) {
            Toast.makeText(getContext(), "Không tìm thấy thông tin shipper. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            return;
        }

        btnStartSession.setEnabled(false);
        btnStartSession.setText("Đang tạo phiên...");

        SessionClient service = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);
        Call<com.ds.deliveryapp.clients.res.BaseResponse<DeliverySession>> call = service.createSessionPrepared(driverId);

        call.enqueue(new Callback<com.ds.deliveryapp.clients.res.BaseResponse<DeliverySession>>() {
            @Override
            public void onResponse(Call<com.ds.deliveryapp.clients.res.BaseResponse<DeliverySession>> call, Response<com.ds.deliveryapp.clients.res.BaseResponse<DeliverySession>> response) {
                btnStartSession.setEnabled(true);
                btnStartSession.setText("Bắt đầu phiên");

                if (response.isSuccessful() && response.body() != null) {
                    com.ds.deliveryapp.clients.res.BaseResponse<DeliverySession> baseResponse = response.body();
                    if (baseResponse.getResult() != null) {
                        DeliverySession session = baseResponse.getResult();
                        Log.d(TAG, "Session created: " + session.getId() + ", Status: " + session.getStatus());
                        Toast.makeText(getContext(), "Đã tạo phiên thành công!", Toast.LENGTH_SHORT).show();
                        
                        // Navigate to TaskFragment to show the session
                        if (getActivity() instanceof MainActivity) {
                            MainActivity mainActivity = (MainActivity) getActivity();
                            mainActivity.navigateToTasks();
                        }
                    } else {
                        String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Không thể tạo phiên";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "Lỗi tạo phiên: " + response.code();
                    if (response.code() == 400) {
                        errorMsg = "Bạn đã có phiên đang hoạt động.";
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to create session: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<com.ds.deliveryapp.clients.res.BaseResponse<DeliverySession>> call, Throwable t) {
                btnStartSession.setEnabled(true);
                btnStartSession.setText("Bắt đầu phiên");
                Log.e(TAG, "Network error creating session: " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }
}
