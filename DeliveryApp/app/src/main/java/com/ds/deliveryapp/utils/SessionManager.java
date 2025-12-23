package com.ds.deliveryapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "delivery_session";
    private static final String KEY_DRIVER_ID = "driver_id";
    private static final String KEY_VEHICLE_TYPE = "vehicle_type"; // BIKE or CAR from database

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveDriverId(String driverId) {
        prefs.edit().putString(KEY_DRIVER_ID, driverId).apply();
    }

    public String getDriverId() {
        return prefs.getString(KEY_DRIVER_ID, null);
    }
    
    public void saveVehicleType(String vehicleType) {
        prefs.edit().putString(KEY_VEHICLE_TYPE, vehicleType).apply();
    }
    
    public String getVehicleType() {
        return prefs.getString(KEY_VEHICLE_TYPE, null);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
