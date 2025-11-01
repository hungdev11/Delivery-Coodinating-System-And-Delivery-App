package com.ds.deliveryapp.auth;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AuthManager {
    private static final String PREFS_NAME = "AuthPrefs";
    private static final String KEY_ACCESS_TOKEN = "accessToken";
    private static final String KEY_REFRESH_TOKEN = "refreshToken";
    private static final String USER_ID = "userId";
    private static final String ROLES = "roles";

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

    public String getUserId() {
        return prefs.getString(USER_ID, null);
    }

    public List<String> getRoles() {
        return new ArrayList<>(prefs.getStringSet(ROLES, null));
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    // Cần gọi phương thức này khi nhận được token mới (khi login hoặc refresh)
    public void saveTokens(String accessToken, String refreshToken, String userId, List<String> roles) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(USER_ID, userId)
                .putStringSet(ROLES, new HashSet<>(roles))
                .apply();
    }

    // Được gọi khi token hết hạn hoặc người dùng logout
    public void clearAuthData() {
        prefs.edit().clear().apply();
    }
}