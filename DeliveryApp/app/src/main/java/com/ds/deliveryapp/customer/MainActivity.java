package com.ds.deliveryapp.customer;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.ds.deliveryapp.ProfileFragment;
import com.ds.deliveryapp.R;
import com.ds.deliveryapp.model.Parcel;
import com.ds.deliveryapp.utils.OnParcelClickListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener, OnParcelClickListener {

    private static final String TAG = "MainActivity_Debug";
    private BottomNavigationView bottomNavView;
    private FrameLayout detailContainer; // Container cho màn hình chi tiết

    private final Fragment listFragment = new ParcelListFragment();
    private final Fragment trackFragment = new ParcelTrackFragment();
    private final Fragment profileFragment = new ProfileFragment();

    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment = listFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customer_main_activity);

        bottomNavView = findViewById(R.id.customer_bottom_nav_view);


        detailContainer = findViewById(R.id.detail_fragment_container);

        View mainContainer = findViewById(R.id.main_fragment_container);
        Log.d(TAG, "ID của mainContainer: " + mainContainer.getId());
        Log.d(TAG, "ID của detailContainer: " + detailContainer.getId());

        bottomNavView.setOnNavigationItemSelectedListener(this);

        fm.beginTransaction().add(R.id.main_fragment_container, profileFragment, "3").hide(profileFragment).commit();
        fm.beginTransaction().add(R.id.main_fragment_container, trackFragment, "2").hide(trackFragment).commit();
        fm.beginTransaction().add(R.id.main_fragment_container, listFragment, "1").commit();
        activeFragment = listFragment;

        fm.addOnBackStackChangedListener(() -> {
            if (fm.getBackStackEntryCount() == 0) {
                detailContainer.setVisibility(View.GONE);
                bottomNavView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (detailContainer.getVisibility() == View.VISIBLE) {
            return false; // Không cho chuyển tab khi đang xem chi tiết
        }

        Log.d(TAG, "Chuyển tab!");

        int itemId = item.getItemId();

        if (itemId == R.id.navigation_list) {
            fm.beginTransaction().hide(activeFragment).show(listFragment).commit();
            activeFragment = listFragment;
            return true;
        } else if (itemId == R.id.navigation_track) {
            fm.beginTransaction().hide(activeFragment).show(trackFragment).commit();
            activeFragment = trackFragment;
            return true;
        } else if (itemId == R.id.nav_profile) {
            fm.beginTransaction().hide(activeFragment).show(profileFragment).commit();
            activeFragment = profileFragment;
            return true;
        }
        return false;
    }

    @Override
    public void onParcelClick(Parcel parcel) {
        ParcelDetailFragment detailFragment = ParcelDetailFragment.newInstance(parcel);

        fm.beginTransaction()
                .add(R.id.detail_fragment_container, detailFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack("detail")
                .commit();

        detailContainer.setVisibility(View.VISIBLE);
        bottomNavView.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Nút Back đã được bấm!");

        if (detailContainer.getVisibility() == View.VISIBLE) {
            Log.d(TAG, "Phát hiện detailContainer đang VISIBLE, tiến hành ẩn nó đi.");

            // --- Pop trước, rồi mới ẩn view ---
            fm.popBackStack("detail", FragmentManager.POP_BACK_STACK_INCLUSIVE);

            detailContainer.setVisibility(View.GONE);
            bottomNavView.setVisibility(View.VISIBLE);

            Log.d(TAG, "Đã ẩn detailContainer. Trạng thái mới: " + detailContainer.getVisibility());
        } else {
            Log.d(TAG, "detailContainer đang GONE, thoát app.");
            super.onBackPressed();
        }
    }

}

