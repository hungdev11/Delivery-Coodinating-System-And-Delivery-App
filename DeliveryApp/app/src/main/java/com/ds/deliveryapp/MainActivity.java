package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.dialog.ProposalPopupDialog;
import com.ds.deliveryapp.enums.ContentType;
import com.ds.deliveryapp.service.GlobalChatService;
import com.ds.deliveryapp.utils.UserInfoLoader;
import com.ds.deliveryapp.widget.ChatFloatingButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements GlobalChatService.ProposalListener {

    private ChatFloatingButton chatFloatingButton;
    private GlobalChatService globalChatService;

    // Cache fragments
    private Map<Integer, String> fragmentTags = new HashMap<>(); // Lưu tag thay vì instance để an toàn hơn
    private Fragment currentFragment = null;
    private static final String TAG_PREFIX = "FRAGMENT_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init Service
        globalChatService = GlobalChatService.getInstance(this);
        globalChatService.addProposalListener(this);
        if (!globalChatService.isConnected()) {
            globalChatService.initialize();
        }

        chatFloatingButton = findViewById(R.id.chat_floating_button);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        // Khôi phục trạng thái nếu xoay màn hình
        if (savedInstanceState != null) {
            // Tìm lại currentFragment từ FragmentManager để tránh null reference
            Fragment savedFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (savedFragment != null) {
                currentFragment = savedFragment;
            }
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            // Chỉ truyền ID và Class, không new instance ở đây
            if (id == R.id.nav_orders) {
                // Luôn hiển thị TaskFragment cho tab Orders
                changeTab(id, TaskFragment.class);
            } else if (id == R.id.nav_map) {
                changeTab(id, MapFragment.class);
            } else if (id == R.id.nav_activity) {
                changeTab(id, ActivityFragment.class);
            } else if (id == R.id.nav_profile) {
                changeTab(id, ProfileFragment.class);
            }
            return true;
        });

        // Set default selection (chỉ chạy lần đầu, không chạy khi xoay màn hình)
        if (savedInstanceState == null) {
            bottomNavigation.setSelectedItemId(R.id.nav_orders);
        }

        UserInfoLoader.loadUserInfo(this);
    }

    /**
     * Hàm xử lý switch tab an toàn
     */
    private void changeTab(int menuId, Class<? extends Fragment> fragmentClass) {
        String tag = TAG_PREFIX + menuId;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        // 1. Tìm fragment trong Manager trước (Xử lý xoay màn hình)
        Fragment targetFragment = fm.findFragmentByTag(tag);

        // 2. Nếu chưa có thì tạo mới
        if (targetFragment == null) {
            try {
                targetFragment = fragmentClass.newInstance();
                // Add với TAG để sau này tìm lại được
                transaction.add(R.id.fragment_container, targetFragment, tag);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        // 3. Ẩn fragment hiện tại
        if (currentFragment != null && currentFragment != targetFragment) {
            transaction.hide(currentFragment);
            transaction.setMaxLifecycle(currentFragment, Lifecycle.State.STARTED);
        }

        // 4. Hiện fragment đích
        transaction.show(targetFragment);
        transaction.setMaxLifecycle(targetFragment, Lifecycle.State.RESUMED);

        currentFragment = targetFragment;
        transaction.commit();
    }

    // ... giữ nguyên onActivityResult, onProposalReceived, onDestroy ...

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("MainActivity", "onActivityResult: " + requestCode);
        super.onActivityResult(requestCode, resultCode, data);
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