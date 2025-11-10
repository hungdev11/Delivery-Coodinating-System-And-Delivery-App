package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ds.deliveryapp.auth.AuthManager;
import com.ds.deliveryapp.clients.UserClient;
import com.ds.deliveryapp.clients.res.UserInfo;
import com.ds.deliveryapp.configs.RetrofitClient;

// Để tải ảnh, bạn cần thêm thư viện như Glide hoặc Picasso vào build.gradle
// ví dụ: implementation 'com.github.bumptech.glide:glide:4.12.0'
// import com.bumptech.glide.Glide;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private UserClient mUserClient;
    private AuthManager mAuthManager;

    // UI Components
    private TextView tvDriverName, tvEmail, tvPhone;
    private ImageView imgAvatar;
    private Button btnLogout;
    private ProgressBar pbLoading;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Find views
        btnLogout = view.findViewById(R.id.btnLogout);
        tvDriverName = view.findViewById(R.id.tvDriverName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        pbLoading = view.findViewById(R.id.pbLoading);

        // Initialize clients and managers
        if (getContext() != null) {
            mUserClient = RetrofitClient.getRetrofitInstance(getContext()).create(UserClient.class);
            mAuthManager = new AuthManager(getContext());
        }

        btnLogout.setOnClickListener(v -> handleLogout());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Fetch profile data
        loadProfileData();
    }

    private void loadProfileData() {
        if (mAuthManager == null) {
            Log.e(TAG, "AuthManager is null.");
            showLoading(false);
            tvDriverName.setText("Lỗi xác thực");
            return;
        }

        // Giả sử AuthManager có phương thức getUserId() để lấy ID người dùng đã đăng nhập
        // Bạn cần tự implement phương thức này trong AuthManager
        String userId = mAuthManager.getUserId();

        if (userId != null && !userId.isEmpty()) {
            getProfile(userId);
        } else {
            Log.e(TAG, "User ID not found. Cannot load profile.");
            showLoading(false);
            tvDriverName.setText("Chưa đăng nhập");
            tvEmail.setText("");
            tvPhone.setText("");
            Toast.makeText(getContext(), "Không thể tải thông tin, vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLogout() {
        if (getContext() == null || getActivity() == null) return;

        if (mAuthManager != null) {
            mAuthManager.clearAuthData();
        }

        Toast.makeText(getContext(), "Đăng xuất thành công", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void getProfile(String userId) {
        showLoading(true);

        Call<UserInfo> call = mUserClient.getUserInfoById(userId);
        call.enqueue(new Callback<UserInfo>() {
            @Override
            public void onResponse(@NonNull Call<UserInfo> call, @NonNull Response<UserInfo> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    UserInfo.UserBasicInfo userInfo = response.body().getResult();
                    updateUI(userInfo);
                } else {
                    Log.w(TAG, "getProfile: API call successful but no data. Code: " + response.code());
                    tvDriverName.setText("Không tìm thấy hồ sơ");
                    Toast.makeText(getContext(), "Không thể tải hồ sơ. Mã lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserInfo> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading user info", t);
                tvDriverName.setText("Lỗi mạng");
                Toast.makeText(getContext(), "Lỗi mạng, không thể tải hồ sơ.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(UserInfo.UserBasicInfo userInfo) {
        if(userInfo == null) return;

        String fullName = userInfo.getFirstName() + " " + userInfo.getLastName();
        tvDriverName.setText(fullName);
        tvEmail.setText(userInfo.getEmail());
        tvPhone.setText(userInfo.getPhone());

        // TODO: Tải ảnh đại diện
        // Giả sử UserInfo có trường avatarUrl, bạn sẽ dùng Glide/Picasso ở đây
        // if (getContext() != null && userInfo.getAvatarUrl() != null) {
        //    Glide.with(getContext())
        //         .load(userInfo.getAvatarUrl())
        //         .circleCrop() // Bo tròn ảnh
        //         .placeholder(R.mipmap.ic_launcher) // Ảnh mặc định
        //         .error(R.mipmap.ic_launcher) // Ảnh khi lỗi
        //         .into(imgAvatar);
        // }
    }

    private void showLoading(boolean isLoading) {
        if (pbLoading != null && tvDriverName != null && tvEmail != null && tvPhone != null) {
            if (isLoading) {
                pbLoading.setVisibility(View.VISIBLE);
                tvDriverName.setVisibility(View.GONE);
                tvEmail.setVisibility(View.GONE);
                tvPhone.setVisibility(View.GONE);
            } else {
                pbLoading.setVisibility(View.GONE);
                tvDriverName.setVisibility(View.VISIBLE);
                tvEmail.setVisibility(View.VISIBLE);
                tvPhone.setVisibility(View.VISIBLE);
            }
        }
    }
}