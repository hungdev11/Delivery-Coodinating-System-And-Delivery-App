package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_orders) selectedFragment = new TaskFragment();
            else if (id == R.id.nav_map) selectedFragment = new MapFragment();
            else if (id == R.id.nav_activity) selectedFragment = new ActivityFragment();
            else if (id == R.id.nav_profile) selectedFragment = new ProfileFragment();

            if (selectedFragment != null)
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();

            return true;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_orders);
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
}