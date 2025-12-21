package com.ds.deliveryapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.deliveryapp.adapter.TasksAdapter;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.DeliverySession;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.utils.FormaterUtil;
import com.ds.deliveryapp.utils.StatusMapper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity to display details of a delivery session and its assignments
 */
public class SessionDetailActivity extends AppCompatActivity implements TasksAdapter.OnTaskClickListener {

    private static final String TAG = "SessionDetailActivity";

    private TextView tvSessionId;
    private TextView tvSessionStatus;
    private TextView tvStartTime;
    private TextView tvEndTime;
    private TextView tvTotalTasks;
    private TextView tvCompletedTasks;
    private TextView tvFailedTasks;
    private RecyclerView recyclerTasks;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private Toolbar toolbar;

    private TasksAdapter adapter;
    private List<DeliveryAssignment> tasks = new ArrayList<>();
    private String sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        // Get sessionId from intent
        sessionId = getIntent().getStringExtra("sessionId");
        if (sessionId == null || sessionId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin phiên", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        tvSessionId = findViewById(R.id.tv_session_id);
        tvSessionStatus = findViewById(R.id.tv_session_status);
        tvStartTime = findViewById(R.id.tv_start_time);
        tvEndTime = findViewById(R.id.tv_end_time);
        tvTotalTasks = findViewById(R.id.tv_total_tasks);
        tvCompletedTasks = findViewById(R.id.tv_completed_tasks);
        tvFailedTasks = findViewById(R.id.tv_failed_tasks);
        recyclerTasks = findViewById(R.id.recyclerTasks);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        // Setup adapter
        adapter = new TasksAdapter(tasks, this);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerTasks.setAdapter(adapter);

        // Load session details and tasks
        loadSessionDetails();
        loadTasks();
    }

    private void loadSessionDetails() {
        SessionClient service = RetrofitClient.getRetrofitInstance(this).create(SessionClient.class);
        Call<BaseResponse<DeliverySession>> call = service.getSessionById(sessionId);

        call.enqueue(new Callback<BaseResponse<DeliverySession>>() {
            @Override
            public void onResponse(Call<BaseResponse<DeliverySession>> call, 
                                   Response<BaseResponse<DeliverySession>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DeliverySession session = response.body().getResult();
                    if (session != null) {
                        displaySessionInfo(session);
                    }
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<DeliverySession>> call, Throwable t) {
                Log.e(TAG, "Failed to load session details", t);
            }
        });
    }

    private void displaySessionInfo(DeliverySession session) {
        // Session ID
        String sessionIdStr = session.getId() != null ? session.getId().toString() : "N/A";
        tvSessionId.setText("Session ID: " + sessionIdStr);

        // Status
        String statusText = StatusMapper.mapSessionStatus(session.getStatus());
        tvSessionStatus.setText("Trạng thái: " + statusText);

        // Start time
        if (session.getStartTime() != null && !session.getStartTime().isEmpty()) {
            tvStartTime.setText("Bắt đầu: " + FormaterUtil.formatDateTime(session.getStartTime()));
        } else {
            tvStartTime.setText("Bắt đầu: --");
        }

        // End time
        if (session.getEndTime() != null && !session.getEndTime().isEmpty()) {
            tvEndTime.setText("Kết thúc: " + FormaterUtil.formatDateTime(session.getEndTime()));
        } else {
            tvEndTime.setText("Kết thúc: --");
        }

        // Task counts
        tvTotalTasks.setText("Tổng: " + session.getTotalTasks());
        tvCompletedTasks.setText("Hoàn thành: " + session.getCompletedTasks());
        tvFailedTasks.setText("Thất bại: " + session.getFailedTasks());
    }

    private void loadTasks() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        SessionClient service = RetrofitClient.getRetrofitInstance(this).create(SessionClient.class);
        Call<BaseResponse<PageResponse<DeliveryAssignment>>> call = service.getTasksBySessionId(
                sessionId, 0, 100);

        call.enqueue(new Callback<BaseResponse<PageResponse<DeliveryAssignment>>>() {
            @Override
            public void onResponse(Call<BaseResponse<PageResponse<DeliveryAssignment>>> call, 
                                   Response<BaseResponse<PageResponse<DeliveryAssignment>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<DeliveryAssignment> pageResponse = response.body().getResult();
                    List<DeliveryAssignment> assignments = pageResponse != null ? pageResponse.content() : null;
                    
                    if (assignments != null && !assignments.isEmpty()) {
                        tasks.clear();
                        tasks.addAll(assignments);
                        adapter.updateTasks(tasks);
                        tvEmptyState.setVisibility(View.GONE);
                        Log.d(TAG, "Loaded " + assignments.size() + " tasks");
                    } else {
                        tasks.clear();
                        adapter.updateTasks(tasks);
                        tvEmptyState.setVisibility(View.VISIBLE);
                        Log.d(TAG, "No tasks found");
                    }
                } else {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    Toast.makeText(SessionDetailActivity.this, 
                            "Không thể tải danh sách đơn hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<PageResponse<DeliveryAssignment>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
                Log.e(TAG, "Failed to load tasks", t);
                Toast.makeText(SessionDetailActivity.this, 
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onTaskClick(DeliveryAssignment task) {
        // Navigate to task/parcel detail if needed
        // For now, just show a toast
        Toast.makeText(this, "Đơn hàng: " + task.getParcelCode(), Toast.LENGTH_SHORT).show();
    }
}
