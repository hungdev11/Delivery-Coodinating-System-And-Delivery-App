package com.ds.deliveryapp.auth;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ds.deliveryapp.LoginActivity;
import com.ds.deliveryapp.clients.AuthClient;
import com.ds.deliveryapp.clients.req.RefreshTokenRequest;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.RefreshTokenResponse;
import com.ds.deliveryapp.configs.RetrofitClient;

import org.jetbrains.annotations.NotNull;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;
import retrofit2.Retrofit;

public class TokenAuthenticator implements Authenticator {

    private final AuthManager authManager;
    private final Context context;
    private static final String TAG = "TokenAuthenticator";

    public TokenAuthenticator(Context context) {
        this.context = context;
        this.authManager = new AuthManager(context);
    }

    @Override
    public Request authenticate(@NotNull Route route, @NotNull Response response) {
        // Nếu Access Token hiện tại đã được gửi nhưng vẫn bị từ chối (401),
        // và đây không phải là cuộc gọi làm mới token, thì thử làm mới.

        String currentAccessToken = authManager.getAccessToken();

        // 1. Kiểm tra nếu yêu cầu ban đầu là yêu cầu làm mới, tránh vòng lặp vô hạn
        if (response.request().header("Authorization") == null ||
                response.request().url().toString().contains("auth/refresh-token")) {
            return null; // Không thể làm mới token, trả về null để báo lỗi
        }

        String refreshToken = authManager.getRefreshToken();
        if (refreshToken == null) {
            Log.e(TAG, "Refresh Token is null. Logging out.");
            logoutAndRedirect();
            return null;
        }

        try {
            Retrofit authRetrofit = RetrofitClient.getRetrofitInstance(context);
            AuthClient authClient = authRetrofit.create(AuthClient.class);

            Call<BaseResponse<RefreshTokenResponse>> call = authClient.refreshToken(new RefreshTokenRequest(refreshToken));

            retrofit2.Response<BaseResponse<RefreshTokenResponse>> refreshResponse = call.execute();

            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                RefreshTokenResponse newTokens = refreshResponse.body().getResult();

                if (newTokens != null && newTokens.getAccessToken() != null) {

                    authManager.saveTokens(
                            newTokens.getAccessToken(),
                            newTokens.getRefreshToken(),
                            authManager.getUserId(),
                            authManager.getRoles()
                    );

                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + newTokens.getAccessToken())
                            .build();
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Token refresh failed: " + e.getMessage());
        }

        logoutAndRedirect();
        return null;
    }

    private void logoutAndRedirect() {
        authManager.clearAuthData();
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
