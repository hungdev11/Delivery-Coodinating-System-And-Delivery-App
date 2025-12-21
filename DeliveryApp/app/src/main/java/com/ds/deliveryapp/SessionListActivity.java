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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ds.deliveryapp.adapter.SessionAdapter;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.DeliverySession;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity to display list of all delivery sessions for the current driver
 */
public class SessionListActivity extends AppCompatActivity implements SessionAdapter.OnSessionClickListener {

    private static final String TAG = "SessionListActivity";

    private RecyclerView recyclerSessions;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SessionAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private Toolbar toolbar;

    private String driverId;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_list);

        // Initialize SessionManager and get driverId
        sessionManager = new SessionManager(this);
        driverId = sessionManager.getDriverId();

        // Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        recyclerSessions = findViewById(R.id.recyclerSessions);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        // Setup adapter
        adapter = new SessionAdapter(this);
        recyclerSessions.setLayoutManager(new LinearLayoutManager(this));
        recyclerSessions.setAdapter(adapter);

        // Setup swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadSessions);

        // Load sessions
        loadSessions();
    }

    private void loadSessions() {
        if (driverId == null || driverId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin tài xế", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(true);

        SessionClient service = RetrofitClient.getRetrofitInstance(this).create(SessionClient.class);
        Call<BaseResponse<List<DeliverySession>>> call = service.getAllSessionsForDeliveryMan(driverId, null);

        call.enqueue(new Callback<BaseResponse<List<DeliverySession>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<DeliverySession>>> call, 
                                   Response<BaseResponse<List<DeliverySession>>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<DeliverySession>> baseResponse = response.body();
                    Log.d(TAG, "Response body: result=" + baseResponse.getResult() + ", data=" + baseResponse.getData());
                    
                    // Try getData() first (some APIs return 'data'), fallback to getResult()
                    List<DeliverySession> sessions = baseResponse.getData();
                    if (sessions == null) {
                        sessions = baseResponse.getResult();
                    }
                    
                    Log.d(TAG, "Sessions list: " + (sessions != null ? sessions.size() : "null"));
                    
                    if (sessions != null && !sessions.isEmpty()) {
                        adapter.setData(sessions);
                        tvEmptyState.setVisibility(View.GONE);
                        Log.d(TAG, "Loaded " + sessions.size() + " sessions");
                    } else {
                        adapter.setData(new ArrayList<>());
                        tvEmptyState.setVisibility(View.VISIBLE);
                        Log.d(TAG, "No sessions found");
                    }
                } else {
                    Toast.makeText(SessionListActivity.this, 
                            "Không thể tải danh sách phiên", Toast.LENGTH_SHORT).show();
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<List<DeliverySession>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                tvEmptyState.setVisibility(View.VISIBLE);
                Log.e(TAG, "Failed to load sessions", t);
                Toast.makeText(SessionListActivity.this, 
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSessionClick(DeliverySession session) {
        // Navigate to session detail
        android.content.Intent intent = new android.content.Intent(this, SessionDetailActivity.class);
        intent.putExtra("sessionId", session.getId() != null ? session.getId().toString() : "");
        startActivity(intent);
    }
}
