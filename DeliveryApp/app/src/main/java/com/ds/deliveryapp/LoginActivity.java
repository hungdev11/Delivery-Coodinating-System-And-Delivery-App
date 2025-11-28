package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.ds.deliveryapp.configs.ServerConfigManager;
import com.ds.deliveryapp.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText edtUsername, edtPassword;
    private Button btnLogin;
    private ProgressBar loadingIndicator;
    private Spinner spinnerServer;
    private Button btnShipper01, btnShipper02, btnShipper03;

    private AuthManager authManager;
    private AuthClient authClient;
    private SessionManager sessionManager;
    private ServerConfigManager serverConfigManager;

    // Default shipper password
    private static final String SHIPPER_PASSWORD = "shipper123";

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Khởi tạo các đối tượng quản lý
        authManager = new AuthManager(this);
        sessionManager = new SessionManager(this);
        serverConfigManager = ServerConfigManager.getInstance(this);
        authClient = RetrofitClient.getAuthRetrofitInstance(this).create(AuthClient.class);

        initViews();
        setupServerPicker();
        setupQuickLoginButtons();

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
        spinnerServer = findViewById(R.id.spinnerServer);
        btnShipper01 = findViewById(R.id.btnShipper01);
        btnShipper02 = findViewById(R.id.btnShipper02);
        btnShipper03 = findViewById(R.id.btnShipper03);
    }

    private void setupServerPicker() {
        // Get server names from ServerConfigManager
        String[] serverNames = ServerConfigManager.getServerNames();
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            serverNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerServer.setAdapter(adapter);
        
        // Set current selection
        int currentIndex = serverConfigManager.getCurrentServerIndex();
        spinnerServer.setSelection(currentIndex);
        
        // Handle server selection changes
        spinnerServer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String serverName = ServerConfigManager.SERVERS[position][0];
                String baseUrl = ServerConfigManager.SERVERS[position][1];
                
                // Only save if different from current
                if (!baseUrl.equals(serverConfigManager.getBaseUrl())) {
                    serverConfigManager.saveServerConfig(serverName, baseUrl);
                    // Recreate auth client with new server
                    authClient = RetrofitClient.getAuthRetrofitInstance(LoginActivity.this).create(AuthClient.class);
                    Log.d(TAG, "Server changed to: " + serverName + " (" + baseUrl + ")");
                    Toast.makeText(LoginActivity.this, "Server: " + serverName, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupQuickLoginButtons() {
        btnShipper01.setOnClickListener(v -> fillCredentials("shipper01", SHIPPER_PASSWORD));
        btnShipper02.setOnClickListener(v -> fillCredentials("shipper02", SHIPPER_PASSWORD));
        btnShipper03.setOnClickListener(v -> fillCredentials("shipper03", SHIPPER_PASSWORD));
    }

    private void fillCredentials(String username, String password) {
        edtUsername.setText(username);
        edtPassword.setText(password);
        // Auto-trigger login after filling credentials
        handleLogin();
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

                    // Lưu driverId vào SessionManager (sử dụng Keycloak user ID làm driverId)
                    sessionManager.saveDriverId(user.getSub());

                    Log.i(TAG, "User info fetched and tokens saved. UserID: " + user.getSub() + ", Roles: " + user.getRoles());
                    Log.i(TAG, "DriverId saved to SessionManager: " + user.getSub());

                    // 2. Điều hướng dựa trên roles
                    navigateToMainApp(user.getRoles());
                } else {
                    // Lỗi (ví dụ: token hết hạn)
                    showLoading(false);
                    Log.e(TAG, "Failed to fetch user info (API Error: " + response.code() + "). Token might be expired.");
                    authManager.clearAuthData(); // Xóa token cũ
                    sessionManager.clear(); // Xóa driverId
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
                sessionManager.clear(); // Xóa driverId
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
            sessionManager.clear(); // Xóa driverId
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
