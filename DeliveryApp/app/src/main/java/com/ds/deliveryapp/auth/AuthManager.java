package com.ds.deliveryapp.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthManager {
    private static final String PREFS_NAME = "AuthPrefs";
    private static final String KEY_ACCESS_TOKEN = "accessToken";
    private static final String KEY_REFRESH_TOKEN = "refreshToken";
    private static final String KEY_DRIVER_ID = "driverId";

    private final SharedPreferences prefs;

    // Khởi tạo AuthManager trong Activity/Fragment bằng cách truyền context
    public AuthManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getDriverId() {
        return prefs.getString(KEY_DRIVER_ID, null);
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    // Cần gọi phương thức này khi nhận được token mới (khi login hoặc refresh)
    public void saveTokens(String accessToken, String refreshToken, String driverId) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_DRIVER_ID, driverId)
                .apply();
    }

    // Được gọi khi token hết hạn hoặc người dùng logout
    public void clearAuthData() {
        prefs.edit().clear().apply();
    }
}