package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.deliveryapp.adapter.TasksAdapter;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.req.SessionFailRequest;
import com.ds.deliveryapp.clients.res.DeliverySession;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Màn hình Nhiệm vụ hôm nay.
 * (API lấy các task của phiên (session) đang hoạt động).
 */
public class TaskFragment extends Fragment implements TasksAdapter.OnTaskClickListener {

    private RecyclerView rvTasks;
    private TasksAdapter adapter;
    private List<DeliveryAssignment> tasks;
    private ProgressBar progressBar;
    private Button btnScanOrder;
    private ImageButton btnSessionMenu;

    private int currentPage = 0;
    private final int pageSize = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    private LinearLayoutManager layoutManager;

    private String activeSessionId = null;

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
        layoutManager = new LinearLayoutManager(getContext());
        rvTasks.setLayoutManager(layoutManager);
        rvTasks.setAdapter(adapter);
        progressBar = view.findViewById(R.id.progress_bar);

        btnScanOrder = view.findViewById(R.id.btnScanOrder);
        btnScanOrder.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), QrScanActivity.class);
            startActivityForResult(intent, SCAN_REQUEST_CODE);
        });

        btnSessionMenu = view.findViewById(R.id.btn_session_menu);
        setupSessionMenu();

        setupPaginationScrollListener();
        resetAndFetchTasks();

        return view;
    }

    // Phương thức tiện ích để reset trạng thái phân trang
    private void resetAndFetchTasks() {
        currentPage = 0;
        isLastPage = false;
        tasks.clear();
        adapter.notifyDataSetChanged();
        //Ẩn nút menu khi tải lại
        if (btnSessionMenu != null) {
            btnSessionMenu.setVisibility(View.GONE);
        }
        activeSessionId = null;
        fetchTodayTasks(currentPage);
    }

    // Xử lý kết quả từ QrScanActivity
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCAN_REQUEST_CODE && resultCode == getActivity().RESULT_OK) {
            Toast.makeText(getContext(), "Cập nhật danh sách nhiệm vụ...", Toast.LENGTH_SHORT).show();
            resetAndFetchTasks();
        }
    }

    public void fetchTodayTasks(int page) {
        if (isLoading || isLastPage) return;

        isLoading = true;
        if (page == 0) {
            progressBar.setVisibility(View.VISIBLE);
        }

        SessionClient service = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);

        List<String> statusFilter = Arrays.asList("IN_PROGRESS");

        Call<PageResponse<DeliveryAssignment>> call = service.getTasksToday(
                DRIVER_ID,
                statusFilter,
                page,
                pageSize
        );

        call.enqueue(new Callback<PageResponse<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<PageResponse<DeliveryAssignment>> call, Response<PageResponse<DeliveryAssignment>> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<DeliveryAssignment> pageResponse = response.body();
                    List<DeliveryAssignment> newTasks = pageResponse.content();

                    isLastPage = pageResponse.last();

                    if (page == 0) {
                        tasks.clear();
                    }

                    tasks.addAll(newTasks);
                    adapter.updateTasks(tasks);

                    // Thêm: Logic lưu Session ID và hiển thị nút menu
                    if (page == 0 && !tasks.isEmpty()) {
                        // tất cả task trong "today" thuộc cùng 1 session
                        activeSessionId = tasks.get(0).getSessionId();
                        if (btnSessionMenu != null) {
                            btnSessionMenu.setVisibility(View.VISIBLE);
                        }
                    }

                    if (tasks.isEmpty() && page == 0) {
                        Toast.makeText(getContext(), "Không có nhiệm vụ nào.", Toast.LENGTH_SHORT).show();
                        if (btnSessionMenu != null) {
                            btnSessionMenu.setVisibility(View.GONE);
                        }
                    } else {
                        Log.d(TAG, "Tasks loaded: Page " + page + ", Size " + newTasks.size());
                    }

                    currentPage++;

                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.code());
                    Toast.makeText(getContext(), "Lỗi tải đơn hàng: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<PageResponse<DeliveryAssignment>> call, Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối mạng.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupPaginationScrollListener() {
        rvTasks.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= pageSize) {

                        fetchTodayTasks(currentPage);
                    }
                }
            }
        });
    }

    // --- MENU PHIÊN (SESSION) ---

    private void setupSessionMenu() {
        btnSessionMenu.setOnClickListener(v -> {
            if (activeSessionId == null) {
                Toast.makeText(getContext(), "Không tìm thấy phiên hoạt động.", Toast.LENGTH_SHORT).show();
                return;
            }

            PopupMenu popup = new PopupMenu(getContext(), v);
            popup.getMenuInflater().inflate(R.menu.session_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_complete_session) {
                    showCompleteSessionDialog();
                    return true;
                } else if (itemId == R.id.menu_fail_session) {
                    showFailSessionDialog();
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    // 1. Hoàn tất phiên
    private void showCompleteSessionDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Hoàn tất phiên")
                .setMessage("Bạn có chắc chắn muốn kết thúc ca làm việc (phiên) này?")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    callCompleteSession();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void callCompleteSession() {
        SessionClient service = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);
        Call<DeliverySession> call = service.completeSession(activeSessionId);

        call.enqueue(new Callback<DeliverySession>() {
            @Override
            public void onResponse(Call<DeliverySession> call, Response<DeliverySession> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã hoàn tất phiên.", Toast.LENGTH_LONG).show();
                    resetAndFetchTasks(); // Tải lại (danh sách sẽ rỗng)
                } else {
                    Toast.makeText(getContext(), "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<DeliverySession> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 2. Hủy phiên (Sự cố)
    private void showFailSessionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Báo cáo sự cố (Hủy phiên)");
        builder.setMessage("Nhập lý do hủy phiên (ví dụ: Hỏng xe, Tai nạn):");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Xác nhận hủy", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(getContext(), "Lý do không được để trống.", Toast.LENGTH_SHORT).show();
            } else {
                callFailSession(reason);
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void callFailSession(String reason) {
        SessionFailRequest requestBody = new SessionFailRequest(reason);

        SessionClient service = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);
        Call<DeliverySession> call = service.failSession(activeSessionId, requestBody);

        call.enqueue(new Callback<DeliverySession>() {
            @Override
            public void onResponse(Call<DeliverySession> call, Response<DeliverySession> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã báo cáo sự cố. Phiên bị hủy.", Toast.LENGTH_LONG).show();
                    resetAndFetchTasks(); // Tải lại (danh sách sẽ rỗng)
                } else {
                    Toast.makeText(getContext(), "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<DeliverySession> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Triển khai phương thức click
    @Override
    public void onTaskClick(DeliveryAssignment task) {
        Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
        intent.putExtra("TASK_DETAIL", task);
        // Sửa: Dùng startActivityForResult để TaskDetail có thể báo lại
        startActivityForResult(intent, SCAN_REQUEST_CODE); // Tái sử dụng SCAN_REQUEST_CODE
    }
}

