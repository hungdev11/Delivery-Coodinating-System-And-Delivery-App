package com.ds.deliveryapp.customer;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.deliveryapp.R;
import com.ds.deliveryapp.adapter.ParcelAdapter;
import com.ds.deliveryapp.model.Parcel;
import com.ds.deliveryapp.utils.OnParcelClickListener;
import com.ds.deliveryapp.viewmodel.ParcelListViewModel;

/**
 * Fragment hiển thị tab "Đơn gửi".
 * (Code gần như giống hệt ReceivedOrdersFragment,
 * chỉ khác ở hàm gọi ViewModel)
 */
public class SendOrdersFragment extends Fragment implements ParcelAdapter.OnItemClickListener {

    private ParcelListViewModel viewModel;
    private RecyclerView recyclerView;
    private ParcelAdapter adapter;
    private OnParcelClickListener mainActivityListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnParcelClickListener) {
            mainActivityListener = (OnParcelClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnParcelClickListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.customer_fragment_tab_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_parcels);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ParcelAdapter(this);
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ParcelListViewModel.class);

        viewModel.getParcelList().observe(getViewLifecycleOwner(), parcels -> {
            adapter.setData(parcels);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.fetchMySentParcels();
    }

    @Override
    public void onItemClick(Parcel parcel) {
        Log.d("FRAG_DEBUGS", "Open detail for parcel " + parcel.getCode());
        if (mainActivityListener != null) {
            mainActivityListener.onParcelClick(parcel);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("LIFE_DEBUG", getClass().getSimpleName() + " resumed");

        // Gọi lại API mỗi khi fragment hiển thị lại
        if (viewModel != null) {
            viewModel.fetchMyParcels();
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.d("LIFE_DEBUG", getClass().getSimpleName() + " paused");
    }

}
