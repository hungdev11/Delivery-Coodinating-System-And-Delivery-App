package com.ds.deliveryapp.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.ds.deliveryapp.customer.ReceiveOrdersFragment;
import com.ds.deliveryapp.customer.SendOrdersFragment;

/**
 * Adapter này quản lý 2 Fragment con (Đơn nhận, Đơn gửi)
 */
public class ParcelListTabAdapter extends FragmentStateAdapter {

    public ParcelListTabAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new ReceiveOrdersFragment();
        } else {
            return new SendOrdersFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // 2 tab
    }
}

