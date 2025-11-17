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
import android.widget.TextView;
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
import com.ds.deliveryapp.clients.res.UpdateNotification;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.service.GlobalChatService;
import com.ds.deliveryapp.utils.SessionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình Nhiệm vụ hôm nay.
 * (API lấy các task của phiên (session) đang hoạt động).
 */
public class TaskFragment extends Fragment implements TasksAdapter.OnTaskClickListener, GlobalChatService.UpdateNotificationListener {

    private RecyclerView rvTasks;
    private TasksAdapter adapter;
    private List<DeliveryAssignment> tasks;
    private ProgressBar progressBar;
    private Button btnScanOrder;
    private ImageButton btnSessionMenu;
    private TextView tvEmptyState;

    private int currentPage = 0;
    private final int pageSize = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    private LinearLayoutManager layoutManager;

    private String activeSessionId = null;
    private String activeSessionStatus = null; // CREATED, IN_PROGRESS, etc.
    private String driverId; // động

    private static final int SCAN_REQUEST_CODE = 1001;
    private static final String TAG = "TaskFragment";

    private SessionManager sessionManager;
    private Button btnStartDelivery;
    private GlobalChatService globalChatService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize GlobalChatService and register update notification listener
        globalChatService = GlobalChatService.getInstance(requireContext());
        globalChatService.addListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        // khởi tạo SessionManager và lấy driverId động
        sessionManager = new SessionManager(requireContext());
        driverId = sessionManager.getDriverId();

        tasks = new ArrayList<>();
        adapter = new TasksAdapter(tasks, this);

        rvTasks = view.findViewById(R.id.recyclerOrders);
        layoutManager = new LinearLayoutManager(getContext());
        rvTasks.setLayoutManager(layoutManager);
        rvTasks.setAdapter(adapter);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);

        // Hiển thị skeleton ngay khi onCreateView (trước khi fetch data)
        adapter.setShowSkeleton(true);

        btnScanOrder = view.findViewById(R.id.btnScanOrder);
        btnScanOrder.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), QrScanActivity.class);
            startActivityForResult(intent, SCAN_REQUEST_CODE);
        });

        btnStartDelivery = view.findViewById(R.id.btnStartDelivery);
        btnStartDelivery.setOnClickListener(v -> startSession());

        btnSessionMenu = view.findViewById(R.id.btn_session_menu);
        setupSessionMenu();

        setupPaginationScrollListener();
        
        // Check for existing session first, show dashboard if none
        checkAndShowDashboardOrTasks();

        return view;
    }

    private void checkAndShowDashboardOrTasks() {
        // First check if there's an active session
        checkActiveSession();
    }
    
    /**
     * Check if there's an active session for this driver
     */
    private void checkActiveSession() {
        SessionClient service = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);
        Call<DeliverySession> call = service.getActiveSession(driverId);
        
        call.enqueue(new Callback<DeliverySession>() {
            @Override
            public void onResponse(Call<DeliverySession> call, Response<DeliverySession> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Active session found - show tasks
                    DeliverySession session = response.body();
                    activeSessionId = session.getId().toString();
                    activeSessionStatus = session.getStatus().toString();
                    Log.d(TAG, "Active session found: " + activeSessionId + ", Status: " + activeSessionStatus);
                    resetAndFetchTasks();
                } else if (response.code() == 204) {
                    // No active session - navigate to dashboard
                    Log.d(TAG, "No active session found. Navigating to dashboard.");
                    navigateToDashboard();
                } else {
                    // Error - try to fetch tasks anyway
                    Log.w(TAG, "Error checking active session: " + response.code());
                    resetAndFetchTasks();
                }
            }

            @Override
            public void onFailure(Call<DeliverySession> call, Throwable t) {
                Log.e(TAG, "Network error checking active session: " + t.getMessage());
                // On error, try to fetch tasks anyway
                resetAndFetchTasks();
            }
        });
    }
    
    /**
     * Navigate to dashboard when no active session
     */
    private void navigateToDashboard() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showDashboard();
        }
    }

    private void resetAndFetchTasks() {
        currentPage = 0;
        isLastPage = false;
        tasks.clear();
        adapter.notifyDataSetChanged();

        if (btnSessionMenu != null) btnSessionMenu.setVisibility(View.GONE);
        fetchSessionTasks(currentPage);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCAN_REQUEST_CODE && resultCode == getActivity().RESULT_OK) {
            Toast.makeText(getContext(), "Cập nhật danh sách nhiệm vụ...", Toast.LENGTH_SHORT).show();
            resetAndFetchTasks();
        }
    }

    public void fetchSessionTasks(int page) {
        if (isLoading || isLastPage) return;
        
        // Need activeSessionId to fetch tasks by sessionId
        if (activeSessionId == null) {
            Log.w(TAG, "No active session ID. Checking for active session first...");
            checkActiveSession();
            return;
        }

        isLoading = true;
        if (page == 0) {
            progressBar.setVisibility(View.VISIBLE);
            if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
            // Hiển thị skeleton khi loading page đầu tiên
            if (adapter != null) {
                adapter.setShowSkeleton(true);
            }
            // Disable buttons during initial load
            setButtonsEnabled(false);
        }

        SessionClient service = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);

        // Use new endpoint: get tasks by sessionId
        Call<PageResponse<DeliveryAssignment>> call = service.getTasksBySessionId(
                activeSessionId,
                page,
                pageSize
        );

        call.enqueue(new Callback<PageResponse<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<PageResponse<DeliveryAssignment>> call, Response<PageResponse<DeliveryAssignment>> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                // Re-enable buttons after load completes
                if (page == 0) {
                    setButtonsEnabled(true);
                }

                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<DeliveryAssignment> pageResponse = response.body();
                    List<DeliveryAssignment> newTasks = pageResponse.content();

                    isLastPage = pageResponse.last();

                    if (page == 0) tasks.clear();

                    tasks.addAll(newTasks);
                    adapter.updateTasks(tasks);
                    
                    // Ẩn skeleton khi đã có data
                    if (page == 0 && adapter != null) {
                        adapter.setShowSkeleton(false);
                    }

                    if (page == 0 && !tasks.isEmpty()) {
                        activeSessionId = tasks.get(0).getSessionId();
                        // Try to get session status from first task or fetch session details
                        fetchSessionStatus();
                        if (btnSessionMenu != null) btnSessionMenu.setVisibility(View.VISIBLE);
                    }

                    if (tasks.isEmpty() && page == 0) {
                        // No tasks - check if there's an active session
                        if (activeSessionId != null) {
                            // Active session exists but no tasks - show empty state
                            checkForCreatedSession();
                        } else {
                            // No active session - navigate to dashboard
                            navigateToDashboard();
                        }
                    } else {
                        // Hide empty state UI when there are tasks
                        if (tvEmptyState != null) {
                            tvEmptyState.setVisibility(View.GONE);
                        }
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
                // Re-enable buttons after load fails
                if (page == 0) {
                    setButtonsEnabled(true);
                }
                // Ẩn skeleton khi có lỗi
                if (page == 0 && adapter != null) {
                    adapter.setShowSkeleton(false);
                }
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

                        fetchSessionTasks(currentPage);
                    }
                }
            }
        });
    }

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
        // Disable buttons during API call
        setButtonsEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        SessionClient service = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);
        Call<DeliverySession> call = service.completeSession(activeSessionId);

        call.enqueue(new Callback<DeliverySession>() {
            @Override
            public void onResponse(Call<DeliverySession> call, Response<DeliverySession> response) {
                progressBar.setVisibility(View.GONE);
                setButtonsEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã hoàn tất phiên.", Toast.LENGTH_LONG).show();
                    // Navigate to dashboard after completing session
                    activeSessionId = null;
                    activeSessionStatus = null;
                    navigateToDashboard();
                } else {
                    Toast.makeText(getContext(), "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DeliverySession> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                setButtonsEnabled(true);
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

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
        // Disable buttons during API call
        setButtonsEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        SessionFailRequest requestBody = new SessionFailRequest(reason);

        SessionClient service = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);
        Call<DeliverySession> call = service.failSession(activeSessionId, requestBody);

        call.enqueue(new Callback<DeliverySession>() {
            @Override
            public void onResponse(Call<DeliverySession> call, Response<DeliverySession> response) {
                progressBar.setVisibility(View.GONE);
                setButtonsEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã báo cáo sự cố. Phiên bị hủy.", Toast.LENGTH_LONG).show();
                    // Navigate to dashboard after failing session
                    activeSessionId = null;
                    activeSessionStatus = null;
                    navigateToDashboard();
                } else {
                    Toast.makeText(getContext(), "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DeliverySession> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                setButtonsEnabled(true);
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkForCreatedSession() {
        // Check if there's an active session (CREATED or IN_PROGRESS)
        if (activeSessionId != null) {
            // Session exists but no tasks - show empty state with appropriate message
            if (tvEmptyState != null) {
                if ("CREATED".equals(activeSessionStatus)) {
                    tvEmptyState.setText("Chưa có nhiệm vụ nào.\nVui lòng quét mã QR để thêm đơn hàng.");
                    // Show "Start Delivery" button if session is CREATED
                    updateUIForSessionStatus();
                } else {
                    tvEmptyState.setText("Không có nhiệm vụ nào.");
                    if (btnStartDelivery != null) btnStartDelivery.setVisibility(View.GONE);
                }
                tvEmptyState.setVisibility(View.VISIBLE);
            }
            // Show session menu if session exists
            if (btnSessionMenu != null && activeSessionId != null) {
                btnSessionMenu.setVisibility(View.VISIBLE);
            }
            Log.d(TAG, "Active session found but no tasks. Session ID: " + activeSessionId + ", Status: " + activeSessionStatus);
        } else {
            // No active session - should navigate to dashboard (handled by checkActiveSession)
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Chưa có phiên làm việc.\nVui lòng bắt đầu phiên để tiếp tục.");
            }
            if (btnSessionMenu != null) btnSessionMenu.setVisibility(View.GONE);
            if (btnStartDelivery != null) btnStartDelivery.setVisibility(View.GONE);
            Log.d(TAG, "No active session found.");
        }
    }

    private void fetchSessionStatus() {
        if (activeSessionId == null) return;

        SessionClient service = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);
        Call<DeliverySession> call = service.getSessionById(activeSessionId);

        call.enqueue(new Callback<DeliverySession>() {
            @Override
            public void onResponse(Call<DeliverySession> call, Response<DeliverySession> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DeliverySession session = response.body();
                    activeSessionStatus = session.getStatus();
                    updateUIForSessionStatus();
                }
            }

            @Override
            public void onFailure(Call<DeliverySession> call, Throwable t) {
                Log.e(TAG, "Failed to fetch session status: " + t.getMessage());
            }
        });
    }

    private void updateUIForSessionStatus() {
        if ("CREATED".equals(activeSessionStatus)) {
            // Show "Start Delivery" button
            if (btnStartDelivery != null) {
                btnStartDelivery.setVisibility(View.VISIBLE);
            }
        } else {
            // Hide "Start Delivery" button for IN_PROGRESS sessions
            if (btnStartDelivery != null) {
                btnStartDelivery.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Enable/disable all buttons during API calls
     */
    private void setButtonsEnabled(boolean enabled) {
        if (btnStartDelivery != null) {
            btnStartDelivery.setEnabled(enabled);
        }
        if (btnScanOrder != null) {
            btnScanOrder.setEnabled(enabled);
        }
        if (btnSessionMenu != null) {
            btnSessionMenu.setEnabled(enabled);
        }
    }

    private void startSession() {
        if (activeSessionId == null) {
            Toast.makeText(getContext(), "Không tìm thấy phiên.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnStartDelivery.setEnabled(false);
        btnStartDelivery.setText("Đang bắt đầu...");

        SessionClient service = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);
        Call<DeliverySession> call = service.startSession(activeSessionId);

        call.enqueue(new Callback<DeliverySession>() {
            @Override
            public void onResponse(Call<DeliverySession> call, Response<DeliverySession> response) {
                btnStartDelivery.setEnabled(true);
                btnStartDelivery.setText("Bắt đầu giao hàng");

                if (response.isSuccessful() && response.body() != null) {
                    DeliverySession session = response.body();
                    activeSessionStatus = session.getStatus();
                    Toast.makeText(getContext(), "Đã bắt đầu giao hàng!", Toast.LENGTH_SHORT).show();
                    updateUIForSessionStatus();
                    resetAndFetchTasks(); // Refresh to show updated status
                } else {
                    Toast.makeText(getContext(), "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DeliverySession> call, Throwable t) {
                btnStartDelivery.setEnabled(true);
                btnStartDelivery.setText("Bắt đầu giao hàng");
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onTaskClick(DeliveryAssignment task) {
        Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
        intent.putExtra("TASK_DETAIL", task);
        startActivityForResult(intent, SCAN_REQUEST_CODE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister update notification listener
        if (globalChatService != null) {
            globalChatService.removeListener(this);
        }
    }

    // ==================== GlobalChatService.UpdateNotificationListener ====================
    
    @Override
    public void onMessageReceived(com.ds.deliveryapp.clients.res.Message message) {
        // Not used in TaskFragment
    }

    @Override
    public void onUnreadCountChanged(int count) {
        // Not used in TaskFragment
    }

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        // Not used in TaskFragment
    }

    @Override
    public void onError(String error) {
        // Not used in TaskFragment
    }

    @Override
    public void onNotificationReceived(String notificationJson) {
        // Not used in TaskFragment
    }

    @Override
    public void onUpdateNotificationReceived(UpdateNotification updateNotification) {
        Log.d(TAG, String.format("📥 Update notification received: type=%s, entityType=%s, entityId=%s, action=%s", 
            updateNotification.getUpdateType(), 
            updateNotification.getEntityType(), 
            updateNotification.getEntityId(), 
            updateNotification.getAction()));
        
        // Handle update notification on UI thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                handleUpdateNotification(updateNotification);
            });
        }
    }
    
    /**
     * Handle update notification and refresh data accordingly
     */
    private void handleUpdateNotification(UpdateNotification updateNotification) {
        if (updateNotification == null) {
            return;
        }
        
        UpdateNotification.EntityType entityType = updateNotification.getEntityType();
        UpdateNotification.ActionType action = updateNotification.getAction();
        String entityId = updateNotification.getEntityId();
        
        // Handle SESSION_UPDATE: refresh session status and tasks
        if (entityType == UpdateNotification.EntityType.SESSION) {
            if (action == UpdateNotification.ActionType.COMPLETED || 
                action == UpdateNotification.ActionType.FAILED || 
                action == UpdateNotification.ActionType.CANCELLED) {
                // Session ended - check session status and navigate to dashboard if needed
                Log.d(TAG, "Session ended (action: " + action + "). Checking session status...");
                checkActiveSession();
            } else if (action == UpdateNotification.ActionType.CREATED || 
                       action == UpdateNotification.ActionType.STATUS_CHANGED) {
                // Session created or status changed - refresh tasks
                Log.d(TAG, "Session updated (action: " + action + "). Refreshing tasks...");
                if (activeSessionId == null || activeSessionId.equals(entityId)) {
                    // Refresh session status first, then refresh tasks
                    checkActiveSession();
                }
            }
        }
        // Handle ASSIGNMENT_UPDATE: refresh tasks list
        else if (entityType == UpdateNotification.EntityType.ASSIGNMENT) {
            if (action == UpdateNotification.ActionType.CREATED || 
                action == UpdateNotification.ActionType.UPDATED || 
                action == UpdateNotification.ActionType.STATUS_CHANGED ||
                action == UpdateNotification.ActionType.COMPLETED ||
                action == UpdateNotification.ActionType.FAILED) {
                // Assignment updated - refresh tasks
                Log.d(TAG, "Assignment updated (action: " + action + "). Refreshing tasks...");
                if (activeSessionId != null) {
                    // Refresh tasks for current session
                    resetAndFetchTasks();
                }
            }
        }
        // Handle PARCEL_UPDATE: refresh tasks list (if parcel status changed)
        else if (entityType == UpdateNotification.EntityType.PARCEL) {
            if (action == UpdateNotification.ActionType.STATUS_CHANGED || 
                action == UpdateNotification.ActionType.UPDATED) {
                // Parcel updated - refresh tasks (parcel status might affect assignment status)
                Log.d(TAG, "Parcel updated (action: " + action + "). Refreshing tasks...");
                if (activeSessionId != null) {
                    // Refresh tasks for current session
                    resetAndFetchTasks();
                }
            }
        }
    }
}
