package com.ds.deliveryapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.ds.deliveryapp.clients.AuthClient;
import com.ds.deliveryapp.clients.req.LoginRequest;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.LoginResponse;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class LoginActivity extends AppCompatActivity {
    private EditText edtUsername, edtPassword;
    private Button btnLogin;

    // --- HẰNG SỐ LƯU TRỮ ---
    private static final String PREFS_NAME = "AuthPrefs";
    private static final String KEY_ACCESS_TOKEN = "accessToken";
    private static final String KEY_REFRESH_TOKEN = "refreshToken";
    private static final String KEY_DRIVER_ID = "driverId"; // ID này sẽ được dùng trong các Fragment khác
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        init();
        setupLoginListener();
    }

    private void init() {
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
    }

    private void setupLoginListener() {
        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String username = edtUsername.getText().toString();
        String password = edtPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tài khoản và mật khẩu.", Toast.LENGTH_SHORT).show();
            return;
        }

        LoginRequest request = new LoginRequest(username, password);
        btnLogin.setEnabled(false);

        Retrofit retrofit = RetrofitClient.getAuthRetrofitInstance();
        AuthClient authClient = retrofit.create(AuthClient.class);

        Call<BaseResponse<LoginResponse>> call = authClient.login(request);

        call.enqueue(new Callback<BaseResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<LoginResponse>> call, Response<BaseResponse<LoginResponse>> response) {
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<LoginResponse> baseResponse = response.body();

                    if (baseResponse.getResult() != null) {
                        LoginResponse loginResponse = baseResponse.getResult();

                        saveAuthData(loginResponse);
                        SessionManager sessionManager = new SessionManager(getApplicationContext());
                        sessionManager.saveDriverId(loginResponse.getUser().getKeycloakId());
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMessage = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Thông tin đăng nhập không hợp lệ.";
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                } else {
                    String error = "Lỗi đăng nhập. Mã lỗi: " + response.code();
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<LoginResponse>> call, Throwable t) {
                btnLogin.setEnabled(true);
                Log.e(TAG, "Network failure: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveAuthData(LoginResponse loginResponse) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(KEY_ACCESS_TOKEN, loginResponse.getAccessToken());
        editor.putString(KEY_REFRESH_TOKEN, loginResponse.getRefreshToken());

//        // Lưu Driver ID (Giả định UserDto có trường 'id' là ID của tài xế)
//        if (loginResponse.getUser() != null && loginResponse.getUser().getId() != null) {
//            editor.putString(KEY_DRIVER_ID, loginResponse.getUser().getId());
//        }

        editor.apply();
    }
}