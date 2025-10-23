package com.ds.deliveryapp.auth;

import android.content.Context;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final AuthManager authManager;

    public AuthInterceptor(Context context) {
        this.authManager = new AuthManager(context);
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String accessToken = authManager.getAccessToken();

        // Nếu có Access Token, thêm nó vào Header
        if (accessToken != null) {
            Request.Builder builder = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken);
            originalRequest = builder.build();
        }

        return chain.proceed(originalRequest);
    }
}
