package com.ds.deliveryapp.service;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.req.LocationUpdateRequest;
import com.ds.deliveryapp.configs.RetrofitClient;

import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Background service để track location mỗi 1 giây
 * Chỉ chạy khi có active session (IN_PROGRESS)
 */
public class LocationTrackingService extends Service {
    private static final String TAG = "LocationTrackingService";
    private static final long LOCATION_UPDATE_INTERVAL_MS = 1000; // 1 second
    private static final float MIN_DISTANCE_CHANGE_METERS = 0f; // Update even if no movement

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Handler handler;
    private Runnable locationUpdateRunnable;
    private String currentSessionId;
    private final AtomicBoolean isTracking = new AtomicBoolean(false);
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public LocationTrackingService getService() {
            return LocationTrackingService.this;
        }
    }

    public static final String EXTRA_SESSION_ID = "EXTRA_SESSION_ID";

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (isTracking.get() && currentSessionId != null) {
                    sendLocationUpdate(location);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(EXTRA_SESSION_ID)) {
            String sessionId = intent.getStringExtra(EXTRA_SESSION_ID);
            if (sessionId != null) {
                startTracking(sessionId);
            }
        }
        return START_STICKY;
    }

    /**
     * Bắt đầu tracking cho một session
     */
    public void startTracking(String sessionId) {
        if (isTracking.get() && sessionId.equals(currentSessionId)) {
            Log.d(TAG, "Already tracking session: " + sessionId);
            return;
        }

        Log.d(TAG, "Starting location tracking for session: " + sessionId);
        currentSessionId = sessionId;
        isTracking.set(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED 
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            stopTracking();
            return;
        }

        // Request location updates
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL_MS,
                MIN_DISTANCE_CHANGE_METERS,
                locationListener
            );
            
            // Also try network provider as fallback
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                LOCATION_UPDATE_INTERVAL_MS,
                MIN_DISTANCE_CHANGE_METERS,
                locationListener
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to start location updates", e);
            stopTracking();
        }
    }

    /**
     * Dừng tracking
     */
    public void stopTracking() {
        Log.d(TAG, "Stopping location tracking");
        isTracking.set(false);
        currentSessionId = null;
        
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception e) {
                Log.e(TAG, "Error removing location updates", e);
            }
        }
    }

    /**
     * Gửi location update lên backend
     */
    private void sendLocationUpdate(Location location) {
        if (currentSessionId == null) {
            return;
        }

        LocationUpdateRequest request = new LocationUpdateRequest(
            location.getLatitude(),
            location.getLongitude(),
            location.hasAccuracy() ? (double) location.getAccuracy() : null,
            location.hasSpeed() ? (double) location.getSpeed() : null,
            System.currentTimeMillis()
        );

        SessionClient sessionClient = RetrofitClient
            .getRetrofitInstance(this)
            .create(SessionClient.class);
        Call<com.ds.deliveryapp.clients.res.BaseResponse<Void>> call = 
            sessionClient.sendLocationUpdate(currentSessionId, request);

        call.enqueue(new Callback<com.ds.deliveryapp.clients.res.BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<com.ds.deliveryapp.clients.res.BaseResponse<Void>> call, 
                                 Response<com.ds.deliveryapp.clients.res.BaseResponse<Void>> response) {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "Failed to send location update: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<com.ds.deliveryapp.clients.res.BaseResponse<Void>> call, Throwable t) {
                Log.e(TAG, "Error sending location update", t);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTracking();
    }

    public boolean isTracking() {
        return isTracking.get();
    }
}
