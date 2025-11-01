package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ds.deliveryapp.auth.AuthManager;
import com.ds.deliveryapp.clients.AuthClient;
import com.ds.deliveryapp.clients.req.LoginRequest;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.KeycloakUserInfoDto;
import com.ds.deliveryapp.clients.res.LoginResponse;
import com.ds.deliveryapp.configs.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText edtUsername, edtPassword;
    private Button btnLogin;
    private ProgressBar loadingIndicator;

    private AuthManager authManager;
    private AuthClient authClient;

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Khởi tạo các đối tượng quản lý
        authManager = new AuthManager(this);
        authClient = RetrofitClient.getAuthRetrofitInstance().create(AuthClient.class);

        initViews();

        // --- BƯỚC 1: KIỂM TRA TOKEN KHI KHỞI ĐỘNG ---
        String existingToken = authManager.getAccessToken();
        if (existingToken != null && !existingToken.isEmpty()) {
            Log.d(TAG, "Token đã tồn tại. Tự động xác thực...");
            showLoading(true);
            // Nếu đã có token, gọi /auth/me ngay để lấy roles và điều hướng
            // Truyền null cho refreshToken vì chúng ta sẽ dùng lại token cũ
            fetchUserInfoAndNavigate(existingToken, null);
        } else {
            Log.d(TAG, "Không có token. Chờ người dùng đăng nhập.");
            // Nếu không có token, mới gán listener cho nút login
            setupLoginListener();
        }
    }

    private void initViews() {
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        loadingIndicator = findViewById(R.id.loading_indicator);
    }

    private void setupLoginListener() {
        btnLogin.setOnClickListener(v -> handleLogin());
    }

    /**
     * Xử lý khi người dùng bấm nút Login.
     */
    private void handleLogin() {
        String username = edtUsername.getText().toString();
        String password = edtPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tài khoản và mật khẩu.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        LoginRequest request = new LoginRequest(username, password);

        // --- BƯỚC 2A: GỌI API /login ---
        authClient.login(request).enqueue(new Callback<BaseResponse<LoginResponse>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<LoginResponse>> call, @NonNull Response<BaseResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    LoginResponse loginResponse = response.body().getResult();

                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công! Đang lấy thông tin...", Toast.LENGTH_SHORT).show();

                    // 2. Gọi /auth/me ngay lập tức, truyền cả 2 token
                    fetchUserInfoAndNavigate(
                            loginResponse.getAccessToken(),
                            loginResponse.getRefreshToken()
                    );

                } else {
                    // Lỗi login
                    showLoading(false);
                    String errorMessage = "Thông tin đăng nhập không hợp lệ.";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMessage = response.body().getMessage();
                    } else if (!response.isSuccessful()) {
                        errorMessage = "Lỗi đăng nhập. Mã lỗi: " + response.code();
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<LoginResponse>> call, @NonNull Throwable t) {
                // Lỗi mạng khi login
                showLoading(false);
                Log.e(TAG, "Network failure on /login: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * BƯỚC 3: GỌI API /auth/me, LƯU VÀ ĐIỀU HƯỚNG
     * (Hàm này được gọi từ cả onCreate và handleLogin)
     *
     * @param accessToken Access token (mới hoặc cũ)
     * @param refreshToken Refresh token (mới nếu vừa login, null nếu tự động login)
     */
    private void fetchUserInfoAndNavigate(String accessToken, @Nullable String refreshToken) {
        String authorizationHeader = "Bearer " + accessToken;

        authClient.getUserInfo(authorizationHeader).enqueue(new Callback<BaseResponse<KeycloakUserInfoDto>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<KeycloakUserInfoDto>> call,
                                   @NonNull Response<BaseResponse<KeycloakUserInfoDto>> response) {

                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    KeycloakUserInfoDto user = response.body().getResult();

                    // 1. Lưu tất cả thông tin
                    // Nếu refreshToken là null (tự động login), lấy token cũ từ AuthManager
                    String finalRefreshToken = (refreshToken != null) ? refreshToken : authManager.getRefreshToken();

                    authManager.saveTokens(
                            accessToken,
                            finalRefreshToken,
                            user.getSub(),
                            user.getRoles()
                    );

                    Log.i(TAG, "User info fetched and tokens saved. UserID: " + user.getSub() + ", Roles: " + user.getRoles());

                    // 2. Điều hướng dựa trên roles
                    navigateToMainApp(user.getRoles());
                } else {
                    // Lỗi (ví dụ: token hết hạn)
                    showLoading(false);
                    Log.e(TAG, "Failed to fetch user info (API Error: " + response.code() + "). Token might be expired.");
                    authManager.clearAuthData(); // Xóa token cũ
                    // (Nếu lỗi xảy ra khi tự động đăng nhập, người dùng sẽ ở lại màn hình Login)
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<KeycloakUserInfoDto>> call, @NonNull Throwable t) {
                // Lỗi mạng khi gọi /auth/me
                showLoading(false);
                Log.e(TAG, "Network error on /auth/me: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "Lỗi mạng khi lấy thông tin tài khoản.", Toast.LENGTH_SHORT).show();
                authManager.clearAuthData(); // Xóa token
            }
        });
    }

    /**
     * BƯỚC 4: HÀM ĐIỀU HƯỚNG
     */
    private void navigateToMainApp(List<String> roles) {
        Intent intent;
        if (roles != null && roles.contains("SHIPPER")) {
            Log.d(TAG, "Role 'SHIPPER' found. Navigating to MainActivity.");
            intent = new Intent(LoginActivity.this, MainActivity.class);

        } else if (roles != null && roles.contains("CLIENT")) {
            Log.d(TAG, "Role 'CLIENT' found. Navigating to customer.MainActivity.");
            intent = new Intent(LoginActivity.this, com.ds.deliveryapp.customer.MainActivity.class);

        } else {
            // Không có role hợp lệ
            showLoading(false);
            Log.e(TAG, "No valid role found. Staying on Login.");
            Toast.makeText(this, "Tài khoản không có quyền hợp lệ.", Toast.LENGTH_SHORT).show();
            authManager.clearAuthData(); // Xóa token
            return;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Đóng LoginActivity
    }

    /**
     * Helper để hiển thị/ẩn ProgressBar và vô hiệu hóa các trường
     */
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            loadingIndicator.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            edtUsername.setEnabled(false);
            edtPassword.setEnabled(false);
        } else {
            loadingIndicator.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            edtUsername.setEnabled(true);
            edtPassword.setEnabled(true);
        }
    }
}

