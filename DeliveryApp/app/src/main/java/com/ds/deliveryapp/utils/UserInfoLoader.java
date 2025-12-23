package com.ds.deliveryapp.utils;

import android.content.Context;
import android.util.Log;

import com.ds.deliveryapp.auth.AuthManager;
import com.ds.deliveryapp.clients.UserClient;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.UserDto;
import com.ds.deliveryapp.configs.RetrofitClient;

import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Utility class to load user info and vehicle type from API
 */
public class UserInfoLoader {
    private static final String TAG = "UserInfoLoader";
    
    /**
     * Load user info and vehicle type from API
     * If user is a shipper, vehicle type will be saved to SessionManager
     */
    public static void loadUserInfo(Context context) {
        AuthManager authManager = new AuthManager(context);
        String accessToken = authManager.getAccessToken();
        
        if (accessToken == null || accessToken.isEmpty()) {
            Log.d(TAG, "No access token available, skipping user info load");
            return;
        }
        
        SessionManager sessionManager = new SessionManager(context);
        UserClient userClient = RetrofitClient.getRetrofitInstance(context).create(UserClient.class);
        String authorizationHeader = "Bearer " + accessToken;
        
        Call<BaseResponse<UserDto>> call = userClient.getCurrentUser(authorizationHeader);
        
        call.enqueue(new Callback<BaseResponse<UserDto>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<UserDto>> call, 
                                 @NonNull Response<BaseResponse<UserDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<UserDto> baseResponse = response.body();
                    if (baseResponse.getResult() != null) {
                        UserDto user = baseResponse.getResult();
                        
                        // Save vehicle type if user is a shipper
                        if (user.getDeliveryMan() != null && user.getDeliveryMan().getVehicleType() != null) {
                            String vehicleType = user.getDeliveryMan().getVehicleType();
                            sessionManager.saveVehicleType(vehicleType);
                            Log.d(TAG, "Vehicle type loaded and saved: " + vehicleType);
                        }
                    }
                } else {
                    Log.w(TAG, "Failed to load user info: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<BaseResponse<UserDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading user info", t);
            }
        });
    }
}
