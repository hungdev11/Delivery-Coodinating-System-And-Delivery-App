package com.ds.deliveryapp;

import android.app.Application;

import com.ds.deliveryapp.auth.AuthManager;
import com.ds.deliveryapp.service.GlobalChatService;

/**
 * Application class to initialize global services
 */
public class DeliveryApplication extends Application {

    private GlobalChatService globalChatService;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize GlobalChatService
        globalChatService = GlobalChatService.getInstance(this);
        
        // Check if user is logged in before connecting
        AuthManager authManager = new AuthManager(this);
        String userId = authManager.getUserId();
        String token = authManager.getAccessToken();
        
        if (userId != null && token != null && !userId.isEmpty() && !token.isEmpty()) {
            globalChatService.initialize();
        }
    }

    public GlobalChatService getGlobalChatService() {
        return globalChatService;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (globalChatService != null) {
            globalChatService.disconnect();
        }
    }
}
