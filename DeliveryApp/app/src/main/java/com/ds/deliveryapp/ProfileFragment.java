package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.ds.deliveryapp.auth.AuthManager;
import com.ds.deliveryapp.clients.UserClient;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.UserDto;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    
    private TextView tvDriverName;
    private TextView tvRole;
    private TextView tvEmail;
    private TextView tvPhone;
    private TextView tvVehicleType;
    private ImageView imgAvatar;
    private AuthManager authManager;
    private UserClient userClient;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        tvDriverName = view.findViewById(R.id.tvDriverName);
        tvRole = view.findViewById(R.id.tvRole);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvVehicleType = view.findViewById(R.id.tvVehicleType);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        
        Button btnLogout = view.findViewById(R.id.btnLogout);
        Button btnMessages = view.findViewById(R.id.btnMessages);
        Button btnManageSessions = view.findViewById(R.id.btnManageSessions);

        btnLogout.setOnClickListener(v -> handleLogout());
        btnMessages.setOnClickListener(v -> openMessagesActivity());
        btnManageSessions.setOnClickListener(v -> openSessionListActivity());

        // Initialize auth manager, session manager and user client
        authManager = new AuthManager(requireContext());
        SessionManager sessionManager = new SessionManager(requireContext());
        userClient = RetrofitClient.getRetrofitInstance(requireContext()).create(UserClient.class);
        
        // Load user info
        loadUserInfo();

        return view;
    }
    
    private void loadUserInfo() {
        String accessToken = authManager.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            Log.w(TAG, "No access token available");
            return;
        }
        
        // Parse JWT token to get user info
        try {
            JsonObject tokenPayload = parseJwtToken(accessToken);
            if (tokenPayload != null) {
                // Extract user info from token
                String firstName = tokenPayload.has("given_name") ? tokenPayload.get("given_name").getAsString() : null;
                String lastName = tokenPayload.has("family_name") ? tokenPayload.get("family_name").getAsString() : null;
                String username = tokenPayload.has("preferred_username") ? tokenPayload.get("preferred_username").getAsString() : null;
                String email = tokenPayload.has("email") ? tokenPayload.get("email").getAsString() : null;
                String name = tokenPayload.has("name") ? tokenPayload.get("name").getAsString() : null;
                String phone = tokenPayload.has("phone_number") ? tokenPayload.get("phone_number").getAsString() : null;
                
                // Extract and display role
                String userRole = null;
                if (tokenPayload.has("realm_access")) {
                    JsonObject realmAccess = tokenPayload.getAsJsonObject("realm_access");
                    if (realmAccess.has("roles")) {
                        List<String> roles = new Gson().fromJson(realmAccess.getAsJsonArray("roles"), 
                            new com.google.gson.reflect.TypeToken<List<String>>(){}.getType());
                        if (roles != null) {
                            if (roles.contains("SHIPPER")) {
                                userRole = "Shipper";
                                // Load delivery info for shipper
                                loadDeliveryInfo(accessToken);
                            } else if (roles.contains("CLIENT") || roles.contains("CUSTOMER")) {
                                userRole = "Khách hàng";
                            }
                        }
                    }
                }
                
                // Update UI with token info
                updateUIFromToken(firstName, lastName, username, email, name, phone);
                updateRole(userRole);
            } else {
                Log.w(TAG, "Failed to parse JWT token");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JWT token", e);
        }
    }
    
    /**
     * Parse JWT token and extract payload
     */
    private JsonObject parseJwtToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                Log.e(TAG, "Invalid JWT token format");
                return null;
            }
            
            // Decode payload (second part)
            String payload = parts[1];
            // Add padding if needed
            while (payload.length() % 4 != 0) {
                payload += "=";
            }
            
            byte[] decodedBytes = Base64.decode(payload, Base64.URL_SAFE);
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);
            
            return new Gson().fromJson(decodedPayload, JsonObject.class);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding JWT token", e);
            return null;
        }
    }
    
    /**
     * Load delivery info from API (only for shippers)
     */
    private void loadDeliveryInfo(String accessToken) {
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
                        // Only update vehicle type from delivery info
                        if (user.getDeliveryMan() != null && user.getDeliveryMan().getVehicleType() != null) {
                            String vehicleType = user.getDeliveryMan().getVehicleType();
                            SessionManager sessionManager = new SessionManager(requireContext());
                            sessionManager.saveVehicleType(vehicleType);
                            updateVehicleType(vehicleType);
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to get delivery info: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<BaseResponse<UserDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading delivery info", t);
            }
        });
    }
    
    /**
     * Update UI with user info from JWT token
     */
    private void updateUIFromToken(String firstName, String lastName, String username, String email, String name, String phone) {
        // Build full name from token
        String fullName = "";
        if (name != null && !name.isEmpty()) {
            fullName = name;
        } else {
            if (firstName != null && !firstName.isEmpty()) {
                fullName = firstName;
            }
            if (lastName != null && !lastName.isEmpty()) {
                if (!fullName.isEmpty()) {
                    fullName += " ";
                }
                fullName += lastName;
            }
        }
        if (fullName.isEmpty() && username != null) {
            fullName = username;
        }
        if (fullName.isEmpty()) {
            fullName = "Người dùng";
        }
        tvDriverName.setText(fullName);
        
        // Update email
        if (email != null && !email.isEmpty()) {
            tvEmail.setText(email);
            tvEmail.setVisibility(View.VISIBLE);
        } else {
            tvEmail.setVisibility(View.GONE);
        }
        
        // Update phone
        if (phone != null && !phone.isEmpty()) {
            tvPhone.setText(phone);
            tvPhone.setVisibility(View.VISIBLE);
        } else {
            tvPhone.setVisibility(View.GONE);
        }
    }
    
    /**
     * Update role display
     */
    private void updateRole(String role) {
        if (role != null && !role.isEmpty()) {
            tvRole.setText(role);
            tvRole.setVisibility(View.VISIBLE);
        } else {
            tvRole.setVisibility(View.GONE);
        }
    }
    
    /**
     * Update vehicle type display (called after loading delivery info)
     */
    private void updateVehicleType(String vehicleType) {
        String vehicleTypeText = "";
        if ("BIKE".equalsIgnoreCase(vehicleType)) {
            vehicleTypeText = "Xe máy";
        } else if ("CAR".equalsIgnoreCase(vehicleType)) {
            vehicleTypeText = "Ô tô";
        } else {
            vehicleTypeText = vehicleType;
        }
        tvVehicleType.setText("Phương tiện: " + vehicleTypeText);
        tvVehicleType.setVisibility(View.VISIBLE);
    }

    private void openMessagesActivity() {
        if (getContext() == null) return;
        
        Intent intent = new Intent(getContext(), ConversationsListActivity.class);
        startActivity(intent);
    }

    private void openSessionListActivity() {
        if (getContext() == null) return;
        
        Intent intent = new Intent(getContext(), SessionListActivity.class);
        startActivity(intent);
    }

    private void handleLogout() {
        if (getContext() == null || getActivity() == null) return;

        AuthManager authManager = new AuthManager(getContext());

        authManager.clearAuthData();

        Toast.makeText(getContext(), "Đăng xuất thành công", Toast.LENGTH_LONG).show();

        //Sử dụng Intent Flags để xóa sạch stack Activity (đóng MainActivity và tất cả Fragments)
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        //Kết thúc Activity cha (MainActivity) sau khi chuyển hướng
        getActivity().finish();
    }
}
