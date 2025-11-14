package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.dialog.ProposalPopupDialog;
import com.ds.deliveryapp.enums.ContentType;
import com.ds.deliveryapp.service.GlobalChatService;
import com.ds.deliveryapp.widget.ChatFloatingButton;

public class MainActivity extends AppCompatActivity implements GlobalChatService.ProposalListener {

    private ChatFloatingButton chatFloatingButton;
    private GlobalChatService globalChatService;
    
    // Cache fragments để giữ state khi switch tabs
    private Map<Integer, Fragment> fragmentCache = new HashMap<>();
    private Fragment currentFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize GlobalChatService
        globalChatService = GlobalChatService.getInstance(this);
        globalChatService.addProposalListener(this);
        if (!globalChatService.isConnected()) {
            globalChatService.initialize();
        }

        // Setup chat floating button
        chatFloatingButton = findViewById(R.id.chat_floating_button);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selectedFragment = null;
            int fragmentKey = 0;

            if (id == R.id.nav_orders) {
                // Check for active session and show appropriate fragment
                selectedFragment = shouldShowDashboard() ? new SessionDashboardFragment() : new TaskFragment();
                fragmentKey = R.id.nav_orders;
            } else if (id == R.id.nav_map) {
                selectedFragment = new MapFragment();
                fragmentKey = R.id.nav_map;
            } else if (id == R.id.nav_activity) {
                selectedFragment = new ActivityFragment();
                fragmentKey = R.id.nav_activity;
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
                fragmentKey = R.id.nav_profile;
            }

            if (selectedFragment != null) {
                switchFragment(fragmentKey, selectedFragment);
            }

            return true;
        });
        // Start with dashboard or tasks based on session status
        if (shouldShowDashboard()) {
            Fragment initialFragment = new SessionDashboardFragment();
            switchFragment(R.id.nav_orders, initialFragment);
        } else {
            bottomNavigation.setSelectedItemId(R.id.nav_orders);
        }
    }

    /**
     * Switch fragment using show/hide instead of replace to preserve state
     */
    private void switchFragment(int fragmentKey, Fragment fragment) {
        Fragment cachedFragment = fragmentCache.get(fragmentKey);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        // Hide current fragment if exists
        if (currentFragment != null) {
            transaction.hide(currentFragment);
            // Set max lifecycle to STARTED for hidden fragment (not destroyed)
            transaction.setMaxLifecycle(currentFragment, Lifecycle.State.STARTED);
        }
        
        if (cachedFragment != null) {
            // Fragment already exists, show it
            transaction.show(cachedFragment);
            transaction.setMaxLifecycle(cachedFragment, Lifecycle.State.RESUMED);
            currentFragment = cachedFragment;
        } else {
            // Fragment doesn't exist, add it
            transaction.add(R.id.fragment_container, fragment, String.valueOf(fragmentKey));
            transaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED);
            fragmentCache.put(fragmentKey, fragment);
            currentFragment = fragment;
        }
        
        transaction.commit();
    }

    private boolean shouldShowDashboard() {
        // Check for active session - if none, show dashboard
        // TaskFragment will also check and navigate if needed
        return false; // Let TaskFragment handle the logic initially
    }
    
    /**
     * Show dashboard fragment (called from TaskFragment when no active session)
     */
    public void showDashboard() {
        Fragment dashboardFragment = new SessionDashboardFragment();
        // Clear cached orders fragment and replace with dashboard
        fragmentCache.remove(R.id.nav_orders);
        switchFragment(R.id.nav_orders, dashboardFragment);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("MainActivity", "onActivityResult - Request Code: " + requestCode + ", Result Code: " + resultCode);

        // 💡 QUAN TRỌNG: KHÔNG XÓA DÒNG NÀY.
        // Dòng super.onActivityResult(...) này sẽ tự động chuyển tiếp sự kiện
        // đến các Fragment đang được host (ví dụ: MapFragment),
        // sau đó MapFragment sẽ chuyển tiếp cho (TaskListDialogFragment).
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Navigate to Tasks fragment (used by SessionDashboardFragment after creating session)
     * Force reload TaskFragment to check for new session
     */
    public void navigateToTasks() {
        // Clear cached orders fragment to force reload
        fragmentCache.remove(R.id.nav_orders);
        
        // Create new TaskFragment (will check for active session)
        Fragment taskFragment = new TaskFragment();
        switchFragment(R.id.nav_orders, taskFragment);
        
        // Update bottom navigation selection
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_orders);
    }

    @Override
    public void onProposalReceived(Message proposalMessage) {
        runOnUiThread(() -> {
            if (proposalMessage != null && proposalMessage.getType() == ContentType.INTERACTIVE_PROPOSAL) {
                ProposalPopupDialog dialog = new ProposalPopupDialog(this, proposalMessage);
                dialog.show();
            }
        });
    }

    @Override
    public void onProposalUpdate(com.ds.deliveryapp.clients.req.ProposalUpdateDTO update) {
        // Handle proposal updates if needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (globalChatService != null) {
            globalChatService.removeProposalListener(this);
        }
        if (chatFloatingButton != null) {
            chatFloatingButton.cleanup();
        }
    }
}
