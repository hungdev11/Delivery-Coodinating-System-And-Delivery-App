package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.dialog.ProposalPopupDialog;
import com.ds.deliveryapp.enums.ContentType;
import com.ds.deliveryapp.service.GlobalChatService;
import com.ds.deliveryapp.widget.ChatFloatingButton;

public class MainActivity extends AppCompatActivity implements GlobalChatService.ProposalListener {

    private ChatFloatingButton chatFloatingButton;
    private GlobalChatService globalChatService;

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
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_orders) {
                // Check for active session and show appropriate fragment
                selectedFragment = shouldShowDashboard() ? new SessionDashboardFragment() : new TaskFragment();
            } else if (id == R.id.nav_map) selectedFragment = new MapFragment();
            else if (id == R.id.nav_activity) selectedFragment = new ActivityFragment();
            else if (id == R.id.nav_profile) selectedFragment = new ProfileFragment();

            if (selectedFragment != null)
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();

            return true;
        });
        // Start with dashboard or tasks based on session status
        if (shouldShowDashboard()) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SessionDashboardFragment())
                    .commit();
        } else {
            bottomNavigation.setSelectedItemId(R.id.nav_orders);
        }
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
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SessionDashboardFragment())
                .commit();
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
     */
    public void navigateToTasks() {
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
