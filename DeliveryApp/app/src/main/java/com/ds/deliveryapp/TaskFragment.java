package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
// ... (Các imports khác giữ nguyên)
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private View loadingLayout; // Giả định có loading spinner trong fragment_tasks

    private static final String DRIVER_ID = "0bbfa6a6-1c0b-4e4f-9e6e-11e36c142ea5";
    private static final String TAG = "TaskFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Sử dụng layout fragment_tasks đã được chỉnh sửa để có RecyclerView
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        tasks = new ArrayList<>();
        adapter = new TasksAdapter(tasks, this);

        // Ánh xạ RecyclerView
        rvTasks = view.findViewById(R.id.rv_tasks_list); // Sửa ID từ recyclerOrders thành rv_tasks_list
        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTasks.setAdapter(adapter);

        // Ánh xạ Progress Bar
        loadingLayout = view.findViewById(R.id.progress_bar);

        fetchTodayTasks(DRIVER_ID);

        return view;
    }

    // Triển khai phương thức click từ TasksAdapter.OnTaskClickListener
    @Override
    public void onOrderClick(DeliveryAssignment task) {
        Intent intent = new Intent(getActivity(), TaskDetailActivity.class);

        // Truyền đối tượng DeliveryAssignment đầy đủ
        intent.putExtra("TASK_DETAIL", task);

        startActivity(intent);
    }

    public void fetchTodayTasks(String driverId) {
        loadingLayout.setVisibility(View.VISIBLE);

        Retrofit retrofit = RetrofitClient.getRetrofitInstance(); // Giả định tồn tại
        SessionClient service = retrofit.create(SessionClient.class); // Giả định tồn tại

        Call<List<DeliveryAssignment>> call = service.getTasksToday(driverId); // Giả định tồn tại

        call.enqueue(new Callback<List<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<List<DeliveryAssignment>> call, Response<List<DeliveryAssignment>> response) {
                loadingLayout.setVisibility(View.GONE);
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
                loadingLayout.setVisibility(View.GONE);
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối mạng.", Toast.LENGTH_LONG).show();
            }
        });
    }
}