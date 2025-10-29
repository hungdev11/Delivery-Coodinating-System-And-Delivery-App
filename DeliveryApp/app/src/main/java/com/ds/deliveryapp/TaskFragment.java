package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.deliveryapp.adapter.TasksAdapter;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.res.PageResponse;
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

    // ⚠️ Biến Phân Trang
    private int currentPage = 0;
    private final int pageSize = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    private Spinner spinnerStatus;
    private LinearLayoutManager layoutManager;

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
        layoutManager = new LinearLayoutManager(getContext()); // ⚠️ Khởi tạo layoutManager
        rvTasks.setLayoutManager(layoutManager);
        rvTasks.setAdapter(adapter);
        progressBar = view.findViewById(R.id.progress_bar);

        btnScanOrder = view.findViewById(R.id.btnScanOrder);
        btnScanOrder.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), QrScanActivity.class);
            startActivityForResult(intent, SCAN_REQUEST_CODE);
        });

        // ⚠️ Thiết lập Scroll Listener cho phân trang
        setupPaginationScrollListener();

        // ⚠️ Tải trang đầu tiên
        resetAndFetchTasks();

        return view;
    }

    // ⚠️ Phương thức tiện ích để reset trạng thái phân trang
    private void resetAndFetchTasks() {
        currentPage = 0;
        isLastPage = false;
        tasks.clear();
        adapter.notifyDataSetChanged();
        fetchTodayTasks(currentPage);
    }

    // ⚠️ Phương thức xử lý kết quả từ QrScanActivity
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCAN_REQUEST_CODE && resultCode == getActivity().RESULT_OK) {
            // Nếu nhận nhiệm vụ thành công, tải lại danh sách từ trang đầu tiên
            Toast.makeText(getContext(), "Cập nhật danh sách nhiệm vụ...", Toast.LENGTH_SHORT).show();
            resetAndFetchTasks();
        }
    }

    // ... (onOrderClick giữ nguyên)

    // ⚠️ Cập nhật logic tải đơn hàng hôm nay với Phân Trang
    public void fetchTodayTasks(int page) {
        if (isLoading || isLastPage) return; // 🛑 Ngăn chặn nếu đang tải hoặc đã hết trang

        isLoading = true;

        // Chỉ hiện ProgressBar ở lần tải đầu tiên (page 0)
        if (page == 0) {
            progressBar.setVisibility(View.VISIBLE);
        }

        Retrofit retrofit = RetrofitClient.getRetrofitInstance(getContext());
        SessionClient service = retrofit.create(SessionClient.class);

        // Giả sử ta muốn tải tất cả trạng thái, truyền null hoặc List rỗng
        List<String> statusFilter = null;

        Call<PageResponse<DeliveryAssignment>> call = service.getTasksToday(
                DRIVER_ID,
                statusFilter, // Không lọc status
                page,
                pageSize
        );

        call.enqueue(new Callback<PageResponse<DeliveryAssignment>>() { // ⚠️ Sửa kiểu trả về
            @Override
            public void onResponse(Call<PageResponse<DeliveryAssignment>> call, Response<PageResponse<DeliveryAssignment>> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<DeliveryAssignment> pageResponse = response.body();
                    List<DeliveryAssignment> newTasks = pageResponse.content();

                    // ⚠️ Cập nhật trạng thái phân trang
                    isLastPage = pageResponse.last();

                    if (page == 0) {
                        tasks.clear();
                    }

                    tasks.addAll(newTasks);
                    adapter.updateTasks(tasks);

                    if (tasks.isEmpty()) {
                        Toast.makeText(getContext(), "Không có đơn hàng hôm nay.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "Tasks loaded: Page " + page + ", Size " + newTasks.size());
                    }

                    currentPage++; // ⚠️ Chuẩn bị cho trang tiếp theo

                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.code());
                    Toast.makeText(getContext(), "Lỗi tải đơn hàng: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<PageResponse<DeliveryAssignment>> call, Throwable t) { // ⚠️ Sửa kiểu trả về
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối mạng.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // ⚠️ Phương thức thiết lập Scroll Listener cho Scroll Vô Hạn
    private void setupPaginationScrollListener() {
        rvTasks.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                // Điều kiện tải trang tiếp theo:
                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= pageSize) {

                        // Kích hoạt tải trang tiếp theo
                        fetchTodayTasks(currentPage);
                    }
                }
            }
        });
    }


    // Triển khai phương thức click từ TasksAdapter.OnTaskClickListener
    @Override
    public void onTaskClick(DeliveryAssignment task) {
        // Giả sử TaskDetailActivity tồn tại
        Intent intent = new Intent(getActivity(), TaskDetailActivity.class);

        // Truyền đối tượng DeliveryAssignment đầy đủ
        // Cần đảm bảo DeliveryAssignment implements Serializable/Parcelable
        intent.putExtra("TASK_DETAIL", task);

        startActivity(intent);
    }
}