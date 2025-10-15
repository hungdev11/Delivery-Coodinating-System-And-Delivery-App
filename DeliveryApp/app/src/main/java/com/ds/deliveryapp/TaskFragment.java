package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.deliveryapp.adapter.TasksAdapter;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TaskFragment extends Fragment implements TasksAdapter.OnTaskClickListener {

    private RecyclerView rvTasks;
    private TasksAdapter adapter;
    private List<DeliveryAssignment> tasks;
    private ProgressBar progressBar;
    private Button btnScanOrder;

    // Yêu cầu code: Cần một mã Request Code duy nhất cho onActivityResult
    private static final int SCAN_REQUEST_CODE = 1001;

    private static final String DRIVER_ID = "0bbfa6a6-1c0b-4e4f-9e6e-11e36c142ea5";
    private static final String TAG = "TaskFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        tasks = new ArrayList<>();
        adapter = new TasksAdapter(tasks, this);

        rvTasks = view.findViewById(R.id.recyclerOrders);
        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTasks.setAdapter(adapter);

        progressBar = view.findViewById(R.id.progress_bar);

        btnScanOrder = view.findViewById(R.id.btnScanOrder);
        btnScanOrder.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), QrScanActivity.class);
            // Dùng startActivityForResult để refresh danh sách nếu nhận nhiệm vụ thành công
            startActivityForResult(intent, SCAN_REQUEST_CODE);
        });

        fetchTodayTasks(DRIVER_ID);

        return view;
    }

    // Phương thức xử lý kết quả từ QrScanActivity (thông qua ParcelDetailActivity)
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Kiểm tra xem Activity là từ Scan và có kết quả thành công không
        if (requestCode == SCAN_REQUEST_CODE && resultCode == getActivity().RESULT_OK) {
            // Nếu nhận nhiệm vụ thành công, tải lại danh sách
            Toast.makeText(getContext(), "Cập nhật danh sách nhiệm vụ...", Toast.LENGTH_SHORT).show();
            fetchTodayTasks(DRIVER_ID);
        }
    }


    // Triển khai phương thức click từ TasksAdapter.OnTaskClickListener
    @Override
    public void onOrderClick(DeliveryAssignment task) {
        // Giả sử TaskDetailActivity tồn tại
        Intent intent = new Intent(getActivity(), TaskDetailActivity.class);

        // Truyền đối tượng DeliveryAssignment đầy đủ
        // Cần đảm bảo DeliveryAssignment implements Serializable/Parcelable
        intent.putExtra("TASK_DETAIL", task);

        startActivity(intent);
    }

    public void fetchTodayTasks(String driverId) {
        progressBar.setVisibility(View.VISIBLE); // Hiển thị ProgressBar

        Retrofit retrofit = RetrofitClient.getSessionRetrofitInstance();
        SessionClient service = retrofit.create(SessionClient.class);

        Call<List<DeliveryAssignment>> call = service.getTasksToday(driverId);

        call.enqueue(new Callback<List<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<List<DeliveryAssignment>> call, Response<List<DeliveryAssignment>> response) {
                progressBar.setVisibility(View.GONE); // Ẩn ProgressBar
                if (response.isSuccessful() && response.body() != null) {
                    List<DeliveryAssignment> newTasks = response.body();

                    tasks.clear();
                    tasks.addAll(newTasks);
                    adapter.updateTasks(tasks);

                    Log.d(TAG, "Tasks received: " + newTasks.size());
                    Toast.makeText(getContext(), "Tải " + newTasks.size() + " đơn hàng thành công.", Toast.LENGTH_SHORT).show();

                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.code());
                    Toast.makeText(getContext(), "Lỗi tải đơn hàng: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<DeliveryAssignment>> call, Throwable t) {
                progressBar.setVisibility(View.GONE); // Ẩn ProgressBar
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối mạng.", Toast.LENGTH_LONG).show();
            }
        });
    }
}