package com.ds.deliveryapp.customer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.ds.deliveryapp.R;
import com.ds.deliveryapp.adapter.ParcelListTabAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Đây là Fragment chứa 2 tab
 */
public class ParcelListFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Sử dụng layout (chứa TabLayout)
        return inflater.inflate(R.layout.customer_fragment_parcel_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager);

        // Dùng getChildFragmentManager() vì đây là Fragment lồng
        ParcelListTabAdapter adapter = new ParcelListTabAdapter(getChildFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);

        // Liên kết TabLayout với ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Đơn nhận" : "Đơn gửi");
        }).attach();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("LIFE_DEBUG", getClass().getSimpleName() + " resumed");
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.d("LIFE_DEBUG", getClass().getSimpleName() + " paused");
    }

}

