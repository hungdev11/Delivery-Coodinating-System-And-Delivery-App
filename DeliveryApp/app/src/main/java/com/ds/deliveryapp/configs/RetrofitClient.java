package com.ds.deliveryapp.configs;

import android.content.Context;

import com.ds.deliveryapp.auth.AuthInterceptor;
import com.ds.deliveryapp.auth.TokenAuthenticator;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit sessionRetrofit;
    private static Retrofit authRetrofit;
    private static Retrofit chatRetrofit;
    private static Context appContext;

    // Timeout configurations (in seconds)
    private static final int CONNECT_TIMEOUT = 30;
    private static final int READ_TIMEOUT = 60;
    private static final int WRITE_TIMEOUT = 60;

    /**
     * Reset all retrofit instances. Should be called when server configuration changes.
     */
    public static synchronized void resetInstances() {
        sessionRetrofit = null;
        authRetrofit = null;
        chatRetrofit = null;
    }

    private static String getGatewayBaseUrl(Context context) {
        return ServerConfigManager.getInstance(context).getGatewayBaseUrl();
    }

    public static Retrofit getRetrofitInstance(Context context) {
        appContext = context.getApplicationContext();
        String gatewayBaseUrl = getGatewayBaseUrl(context);
        
        if (sessionRetrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .addInterceptor(new AuthInterceptor(context))
                    .authenticator(new TokenAuthenticator(context))
                    .build();

            // 2. Khởi tạo Retrofit
            sessionRetrofit = new Retrofit.Builder()
                    .baseUrl(gatewayBaseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return sessionRetrofit;
    }

    public static Retrofit getAuthRetrofitInstance(Context context) {
        String gatewayBaseUrl = getGatewayBaseUrl(context);
        
        if (authRetrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .build();

            authRetrofit = new Retrofit.Builder()
                    .baseUrl(gatewayBaseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return authRetrofit;
    }

    /**
     * @deprecated Use {@link #getAuthRetrofitInstance(Context)} instead
     */
    @Deprecated
    public static Retrofit getAuthRetrofitInstance() {
        if (appContext != null) {
            return getAuthRetrofitInstance(appContext);
        }
        // Fallback to default URL for backward compatibility
        if (authRetrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .build();

            authRetrofit = new Retrofit.Builder()
                    .baseUrl(ServerConfigManager.DEFAULT_BASE_URL + "/api/v1/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return authRetrofit;
    }

    public static Retrofit getChatRetrofitInstance(Context context) {
        String gatewayBaseUrl = getGatewayBaseUrl(context);
        
        if (chatRetrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .build();

            chatRetrofit = new Retrofit.Builder()
                    .baseUrl(gatewayBaseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return chatRetrofit;
    }

    /**
     * @deprecated Use {@link #getChatRetrofitInstance(Context)} instead
     */
    @Deprecated
    public static Retrofit getChatRetrofitInstance() {
        if (appContext != null) {
            return getChatRetrofitInstance(appContext);
        }
        // Fallback to default URL for backward compatibility
        if (chatRetrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .build();

            chatRetrofit = new Retrofit.Builder()
                    .baseUrl(ServerConfigManager.DEFAULT_BASE_URL + "/api/v1/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return chatRetrofit;
    }
}
