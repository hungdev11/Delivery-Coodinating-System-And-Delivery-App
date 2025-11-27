package com.ds.deliveryapp.configs;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages server configuration storage and retrieval.
 * Stores the selected server URL in SharedPreferences.
 */
public class ServerConfigManager {
    private static final String PREF_NAME = "server_config";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_SERVER_NAME = "server_name";
    
    // Default server
    public static final String DEFAULT_BASE_URL = "https://localweb.phuongy.works";
    public static final String DEFAULT_SERVER_NAME = "localweb.phuongy.works";
    
    // Available servers (display name -> base URL)
    public static final String[][] SERVERS = {
        {"localweb.phuongy.works", "https://localweb.phuongy.works"},
        {"localserver.phuongy.works", "https://localserver.phuongy.works"},
        {"project.phuongy.works", "https://project.phuongy.works"}
    };
    
    private final SharedPreferences prefs;
    private static ServerConfigManager instance;
    
    private ServerConfigManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized ServerConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new ServerConfigManager(context);
        }
        return instance;
    }
    
    /**
     * Get the current base URL for API calls
     */
    public String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
    }
    
    /**
     * Get the current server name for display
     */
    public String getServerName() {
        return prefs.getString(KEY_SERVER_NAME, DEFAULT_SERVER_NAME);
    }
    
    /**
     * Get the WebSocket URL based on the current base URL
     */
    public String getWebSocketUrl() {
        String baseUrl = getBaseUrl();
        // Convert https:// to wss://
        String wsUrl = baseUrl.replace("https://", "wss://").replace("http://", "ws://");
        return wsUrl + "/ws/websocket";
    }
    
    /**
     * Get the gateway base URL for API calls
     */
    public String getGatewayBaseUrl() {
        return getBaseUrl() + "/api/v1/";
    }
    
    /**
     * Save the selected server configuration
     */
    public void saveServerConfig(String serverName, String baseUrl) {
        prefs.edit()
            .putString(KEY_SERVER_NAME, serverName)
            .putString(KEY_BASE_URL, baseUrl)
            .apply();
        
        // Reset retrofit instances when server changes
        RetrofitClient.resetInstances();
    }
    
    /**
     * Get the index of the current server in the SERVERS array
     */
    public int getCurrentServerIndex() {
        String currentUrl = getBaseUrl();
        for (int i = 0; i < SERVERS.length; i++) {
            if (SERVERS[i][1].equals(currentUrl)) {
                return i;
            }
        }
        return 0; // Default to first server
    }
    
    /**
     * Get display names of all available servers
     */
    public static String[] getServerNames() {
        String[] names = new String[SERVERS.length];
        for (int i = 0; i < SERVERS.length; i++) {
            names[i] = SERVERS[i][0];
        }
        return names;
    }
}
