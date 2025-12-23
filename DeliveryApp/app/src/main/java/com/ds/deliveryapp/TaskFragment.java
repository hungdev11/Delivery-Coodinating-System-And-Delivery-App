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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.snackbar.Snackbar;

import com.ds.deliveryapp.ReturnToWarehouseActivity;
import com.ds.deliveryapp.adapter.TasksAdapter;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.req.SessionFailRequest;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.DeliverySession;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.clients.res.UpdateNotification;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.model.DeliveryProof;
import com.ds.deliveryapp.service.GlobalChatService;
import com.ds.deliveryapp.utils.SessionManager;
import com.google.gson.Gson;

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
    private final Gson gson = new Gson();
    private RecyclerView rvTasks;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TasksAdapter adapter;
    private List<DeliveryAssignment> tasks;
    private ProgressBar progressBar;
    private Button btnScanOrder;
    private ImageButton btnSessionMenu;
    private boolean isSessionInIncidentMode = false;

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
    private static final int SCAN_TRANSFER_REQUEST_CODE = 1003;
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

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Pull-to-refresh: reset and fetch tasks
            resetAndFetchTasks();
        });
        
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

    private boolean hasUnfinishedTasks() {
        for (DeliveryAssignment task : tasks) {
            String status = task.getStatus();
            if (!"COMPLETED".equals(status)
                    && !"FAILED".equals(status)
                    && !"DELAYED".equals(status)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Check if there's an active session for this driver
     */
    private void checkActiveSession() {
        SessionClient service = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);
        Call<BaseResponse<DeliverySession>> call = service.getActiveSession(driverId);
        
        call.enqueue(new Callback<BaseResponse<DeliverySession>>() {
            @Override
            public void onResponse(Call<BaseResponse<DeliverySession>> call, Response<BaseResponse<DeliverySession>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<DeliverySession> baseResponse = response.body();
                    // Check if result exists (active session found)
                    if (baseResponse.getResult() != null) {
                        // Active session found - show tasks
                        DeliverySession session = baseResponse.getResult();
                        activeSessionId = session.getId() != null ? session.getId().toString() : null;
                        activeSessionStatus = session.getStatus() != null ? session.getStatus() : "UNKNOWN";
                        
                        // If session is COMPLETED or FAILED, navigate to dashboard
                        if ("COMPLETED".equals(activeSessionStatus) || "FAILED".equals(activeSessionStatus)) {
                            Log.d(TAG, "Session is " + activeSessionStatus + ", navigating to dashboard");
                            activeSessionId = null;
                            activeSessionStatus = null;
                            navigateToDashboard();
                            return;
                        }
                        
                        Log.d(TAG, "Active session found: " + activeSessionId + ", Status: " + activeSessionStatus);
                        resetAndFetchTasks();
                    } else {
                        // No active session (result is null, message indicates no session)
                        Log.d(TAG, "No active session found: " + baseResponse.getMessage());
                        navigateToDashboard();
                    }
                } else {
                    // Error - navigate to dashboard
                    Log.w(TAG, "Error checking active session: " + response.code());
                    navigateToDashboard();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<DeliverySession>> call, Throwable t) {
                Log.e(TAG, "Network error checking active session: " + t.getMessage());
                // On error, navigate to dashboard
                navigateToDashboard();
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
            // Check if task was updated
            if (data != null && data.hasExtra("UPDATED_TASK")) {
                DeliveryAssignment updatedTask = (DeliveryAssignment) data.getSerializableExtra("UPDATED_TASK");
                String newStatus = data.getStringExtra("NEW_STATUS");
                
                if (updatedTask != null && newStatus != null) {
                    // Update local task immediately (optimistic update)
                    updateLocalTaskStatus(updatedTask.getParcelId(), newStatus);
                    Log.d(TAG, "✅ Task updated locally: parcelId=" + updatedTask.getParcelId() + ", status=" + newStatus);
                }
            }
            
            // Refresh tasks from server to ensure consistency
            Toast.makeText(getContext(), "Cập nhật danh sách nhiệm vụ...", Toast.LENGTH_SHORT).show();
            resetAndFetchTasks();
        } else if (requestCode == SCAN_TRANSFER_REQUEST_CODE && resultCode == getActivity().RESULT_OK) {
            // Transfer parcel accepted successfully
            Toast.makeText(getContext(), "Đã nhận đơn chuyển giao thành công.", Toast.LENGTH_SHORT).show();
            resetAndFetchTasks();
        } else if (requestCode == 9002 && resultCode == getActivity().RESULT_OK) {
            // ReturnToWarehouseActivity completed successfully
            Toast.makeText(getContext(), "Đã xác nhận trả hàng về kho!", Toast.LENGTH_SHORT).show();
            resetAndFetchTasks();
        }
    }

    public void fetchSessionTasks(int page) {
        if (isLoading || isLastPage) return;
        
        // Need activeSessionId to fetch tasks by sessionId
        if (activeSessionId == null) {
            Log.w(TAG, "No active session ID. Cannot fetch tasks.");
            // Don't call checkActiveSession again to avoid infinite loop
            // Show empty state instead
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Chưa có phiên làm việc.");
            }
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
        Call<BaseResponse<PageResponse<DeliveryAssignment>>> call = service.getTasksBySessionId(
                activeSessionId,
                page,
                pageSize
        );

        call.enqueue(new Callback<BaseResponse<PageResponse<DeliveryAssignment>>>() {
            @Override
            public void onResponse(Call<BaseResponse<PageResponse<DeliveryAssignment>>> call, Response<BaseResponse<PageResponse<DeliveryAssignment>>> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false); // Stop pull-to-refresh animation
                // Re-enable buttons after load completes
                if (page == 0) {
                    setButtonsEnabled(true);
                }

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<PageResponse<DeliveryAssignment>> baseResponse = response.body();
                    if (baseResponse.getResult() == null) {
                        // Error response
                        String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Không thể tải danh sách nhiệm vụ";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PageResponse<DeliveryAssignment> pageResponse = baseResponse.getResult();
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
                        // No tasks - mark as last page to prevent infinite loading
                        isLastPage = true;
                        
                        // Check if there's an active session
                        if (activeSessionId != null) {
                            // Active session exists but no tasks - show appropriate UI
                            if ("IN_PROGRESS".equals(activeSessionStatus)) {
                                // Session is IN_PROGRESS but all tasks are done
                                // Show button to complete session
                                if (tvEmptyState != null) {
                                    tvEmptyState.setVisibility(View.VISIBLE);
                                    tvEmptyState.setText("Tất cả đơn hàng đã hoàn tất!\nVui lòng kết thúc phiên để hoàn thành ca làm việc.");
                                }
                                // Show session menu to allow manual completion
                                if (btnSessionMenu != null) {
                                    btnSessionMenu.setVisibility(View.VISIBLE);
                                }
                            } else {
                                // CREATED session with no tasks yet
                                checkForCreatedSession();
                            }
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
            public void onFailure(Call<BaseResponse<PageResponse<DeliveryAssignment>>> call, Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false); // Stop pull-to-refresh animation
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
                if (itemId == R.id.menu_manage_sessions) {
                    // Navigate to session list
                    android.content.Intent intent = new android.content.Intent(getContext(), SessionListActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.menu_complete_session) {
                    if (isSessionInIncidentMode) {
                        Toast.makeText(
                                getContext(),
                                "Phiên đang xử lý sự cố, không thể hoàn tất.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return true;
                    }
                    showCompleteSessionDialog();
                    return true;
                } else if (itemId == R.id.menu_fail_session) {
                    showFailSessionDialog();
                    return true;
                }
//                } else if (itemId == R.id.menu_show_transfer_qr) {
//                    showTransferQRCode();
//                    return true;
//                } else if (itemId == R.id.menu_scan_transfer_qr) {
//                    showSelectParcelForTransferDialog();
//                    return true;
//                }
                return false;
            });
            popup.show();
        });
    }

    private void showCompleteSessionDialog() {

        if (hasUnfinishedTasks()) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Chưa thể kết thúc phiên")
                    .setMessage(
                            "Vẫn còn đơn hàng đang giao.\n" +
                                    "Bạn chỉ có thể trả hàng về kho khi tất cả các đơn còn lại đều bị trễ hoặc thất bại."
                    )
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Lúc này mới hợp lệ để kiểm tra FAILED / DELAYED
        checkReturnedProofsBeforeComplete();
    }


    private void checkReturnedProofsBeforeComplete() {
        // Filter FAILED and DELAYED tasks
        List<DeliveryAssignment> failedOrDelayedTasks = new ArrayList<>();
        for (DeliveryAssignment task : tasks) {
            if ("FAILED".equals(task.getStatus()) || "DELAYED".equals(task.getStatus())) {
                failedOrDelayedTasks.add(task);
            }
        }
        
        if (failedOrDelayedTasks.isEmpty()) {
            // No failed/delayed tasks, proceed with completion
            showCompleteConfirmationDialog();
            return;
        }
        
        // Check proofs for each failed/delayed task
        checkProofsForTasks(failedOrDelayedTasks, 0);
    }
    
    private void checkProofsForTasks(List<DeliveryAssignment> tasksToCheck, int index) {
        if (index >= tasksToCheck.size()) {
            // All tasks checked, proceed with completion
            showCompleteConfirmationDialog();
            return;
        }
        
        DeliveryAssignment task = tasksToCheck.get(index);
        if (task.getAssignmentId() == null || task.getAssignmentId().isEmpty()) {
            // Skip if no assignmentId
            checkProofsForTasks(tasksToCheck, index + 1);
            return;
        }
        
        SessionClient service = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);
        service.getProofsByAssignment(task.getAssignmentId()).enqueue(new retrofit2.Callback<com.ds.deliveryapp.clients.res.BaseResponse<java.util.List<com.ds.deliveryapp.model.DeliveryProof>>>() {
            @Override
            public void onResponse(retrofit2.Call<com.ds.deliveryapp.clients.res.BaseResponse<java.util.List<com.ds.deliveryapp.model.DeliveryProof>>> call, 
                                 retrofit2.Response<com.ds.deliveryapp.clients.res.BaseResponse<java.util.List<com.ds.deliveryapp.model.DeliveryProof>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    List<com.ds.deliveryapp.model.DeliveryProof> proofs = response.body().getResult();
                    boolean hasReturnedProof = false;
                    for (com.ds.deliveryapp.model.DeliveryProof proof : proofs) {
                        if ("RETURNED".equals(proof.getType())) {
                            hasReturnedProof = true;
                            break;
                        }
                    }
                    
                    if (!hasReturnedProof) {
                        // Found a task without RETURNED proof
                        showReturnToWarehouseRequiredDialog(task);
                        return;
                    }
                }
                
                // Check next task
                checkProofsForTasks(tasksToCheck, index + 1);
            }
            
            @Override
            public void onFailure(retrofit2.Call<com.ds.deliveryapp.clients.res.BaseResponse<java.util.List<com.ds.deliveryapp.model.DeliveryProof>>> call, Throwable t) {
                Log.e(TAG, "Failed to check proofs for task " + task.getAssignmentId(), t);
                // On error, continue checking next task
                checkProofsForTasks(tasksToCheck, index + 1);
            }
        });
    }
    
    private void showReturnToWarehouseRequiredDialog(DeliveryAssignment task) {
        new AlertDialog.Builder(getContext())
                .setTitle("Chưa xác nhận trả hàng về kho")
                .setMessage("Đơn hàng " + task.getParcelCode() + " chưa có bằng chứng trả về kho.\nVui lòng quét lại đơn hàng để xác nhận đã trả về kho trước khi kết thúc phiên.")
                .setPositiveButton("Xác nhận về kho", (dialog, which) -> {
                    // Open ReturnToWarehouseActivity
                    Intent intent = new Intent(getContext(), ReturnToWarehouseActivity.class);
                    intent.putExtra(ReturnToWarehouseActivity.EXTRA_ASSIGNMENT_ID, task.getAssignmentId());
                    startActivityForResult(intent, 9002); // Different request code
                })
                .setNegativeButton("Hủy", null)
                .setCancelable(false)
                .show();
    }
    
    private void showCompleteConfirmationDialog() {
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
        Call<BaseResponse<DeliverySession>> call = service.completeSession(activeSessionId);

        call.enqueue(new Callback<BaseResponse<DeliverySession>>() {
            @Override
            public void onResponse(Call<BaseResponse<DeliverySession>> call, Response<BaseResponse<DeliverySession>> response) {
                progressBar.setVisibility(View.GONE);
                setButtonsEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<DeliverySession> baseResponse = response.body();
                    if (baseResponse.getResult() != null) {
                        Toast.makeText(getContext(), "Đã hoàn tất phiên.", Toast.LENGTH_LONG).show();
                        // Navigate to dashboard after completing session
                        activeSessionId = null;
                        activeSessionStatus = null;
                        navigateToDashboard();
                    } else {
                        String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Không thể hoàn tất phiên";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<DeliverySession>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                setButtonsEnabled(true);
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Show QR code for transferring parcels
     */
    private void showTransferQRCode() {
        if (activeSessionId == null) {
            Toast.makeText(getContext(), "Không tìm thấy phiên hoạt động.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(getContext(), SessionQRDisplayActivity.class);
        intent.putExtra(SessionQRDisplayActivity.EXTRA_SESSION_ID, activeSessionId);
        startActivity(intent);
    }
    
    /**
     * Show dialog to scan QR and accept transferred parcels
     * Flow: Scan QR -> Show list of ON_ROUTE parcels from scanned session -> Select to accept
     */
    private void showSelectParcelForTransferDialog() {
        // Start QR scan activity (no need to select parcel first)
        Intent intent = new Intent(getContext(), SessionQRScanActivity.class);
        startActivityForResult(intent, SCAN_TRANSFER_REQUEST_CODE);
    }


    private boolean hasInProgressTasks() {
        for (DeliveryAssignment task : tasks) {
            if ("IN_PROGRESS".equals(task.getStatus())) {
                return true;
            }
        }
        return false;
    }

    private void showFailSessionDialog() {

        // ===== PHASE 2 =====
        if (!hasInProgressTasks()) {
            // Không hỏi lý do nữa
            callFailSession(null);
            return;
        }

        // ===== PHASE 1 =====
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle("Trả tất cả đơn về kho");

        builder.setMessage(
                "Hệ thống sẽ:\n\n" +
                        "• Đánh dấu tất cả đơn đang giao là THẤT BẠI\n" +
                        "Sau khi xử lý trả tất cả hàng về kho, bạn cần thao tác lại để kết thúc phiên."
        );

        final EditText input = new EditText(getContext());
        input.setHint("Nhập lý do (bắt buộc)");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setMinLines(2);
        input.setPadding(32, 24, 32, 24);

        builder.setView(input);

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String reason = input.getText().toString().trim();

            if (reason.isEmpty()) {
                Toast.makeText(getContext(),
                        "Vui lòng nhập lý do",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            callFailSession(reason); // phase 1
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void callFailSession(@Nullable String reason) {

        setButtonsEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        SessionFailRequest requestBody = new SessionFailRequest(reason);

        SessionClient service = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);
        Call<BaseResponse<DeliverySession>> call =
                service.failSession(activeSessionId, requestBody);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<BaseResponse<DeliverySession>> call,
                                   Response<BaseResponse<DeliverySession>> response) {

                progressBar.setVisibility(View.GONE);
                setButtonsEnabled(true);
                Log.e(TAG, "Response: " + gson.toJson(response.body()));
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(getContext(), "Không thể hủy phiên", Toast.LENGTH_SHORT).show();
                    return;
                }
                isSessionInIncidentMode = true;
                // ===== PHASE 1 =====
                if (hasInProgressTasks()) {
                    // reload task list
                    resetAndFetchTasks();
                    return;
                }

                // ===== PHASE 2 =====
                activeSessionId = null;
                activeSessionStatus = null;
                navigateToDashboard();
            }

            @Override
            public void onFailure(Call<BaseResponse<DeliverySession>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                setButtonsEnabled(true);
                Toast.makeText(getContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
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
        Call<BaseResponse<DeliverySession>> call = service.getSessionById(activeSessionId);

        call.enqueue(new Callback<BaseResponse<DeliverySession>>() {
            @Override
            public void onResponse(Call<BaseResponse<DeliverySession>> call, Response<BaseResponse<DeliverySession>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<DeliverySession> baseResponse = response.body();
                    if (baseResponse.getResult() != null) {
                        DeliverySession session = baseResponse.getResult();
                        activeSessionStatus = session.getStatus() != null ? session.getStatus() : "UNKNOWN";
                        
                        // If session is completed/failed, navigate to dashboard
                        if ("COMPLETED".equals(activeSessionStatus) || "FAILED".equals(activeSessionStatus)) {
                            Log.d(TAG, "Session " + activeSessionId + " is " + activeSessionStatus + ". Navigating to dashboard.");
                            showLightNotification("Phiên giao hàng đã kết thúc");
                            activeSessionId = null;
                            activeSessionStatus = null;
                            navigateToDashboard();
                            return;
                        }
                        
                        updateUIForSessionStatus();
                        
                        // Check if all tasks are complete for IN_PROGRESS session
                        if ("IN_PROGRESS".equals(activeSessionStatus) && tasks.isEmpty()) {
                            // Show message to complete session
                            if (tvEmptyState != null) {
                                tvEmptyState.setVisibility(View.VISIBLE);
                                tvEmptyState.setText("Tất cả đơn hàng đã hoàn tất!\nVui lòng kết thúc phiên để hoàn thành ca làm việc.");
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<DeliverySession>> call, Throwable t) {
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
            // Show scan order button for CREATED session (can still scan new parcels)
            if (btnScanOrder != null) {
                btnScanOrder.setVisibility(View.VISIBLE);
            }
        } else {
            // Hide "Start Delivery" button for IN_PROGRESS sessions
            if (btnStartDelivery != null) {
                btnStartDelivery.setVisibility(View.GONE);
            }
            // Hide scan order button for IN_PROGRESS session (can only transfer parcels)
            if (btnScanOrder != null) {
                btnScanOrder.setVisibility(View.GONE);
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
        Call<BaseResponse<DeliverySession>> call = service.startSession(activeSessionId);

        call.enqueue(new Callback<BaseResponse<DeliverySession>>() {
            @Override
            public void onResponse(Call<BaseResponse<DeliverySession>> call, Response<BaseResponse<DeliverySession>> response) {
                btnStartDelivery.setEnabled(true);
                btnStartDelivery.setText("Bắt đầu giao hàng");

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<DeliverySession> baseResponse = response.body();
                    if (baseResponse.getResult() != null) {
                        DeliverySession session = baseResponse.getResult();
                        activeSessionStatus = session.getStatus() != null ? session.getStatus() : "UNKNOWN";
                        Toast.makeText(getContext(), "Đã bắt đầu giao hàng!", Toast.LENGTH_SHORT).show();
                        updateUIForSessionStatus();
                        resetAndFetchTasks(); // Refresh to show updated status
                    } else {
                        String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Không thể bắt đầu phiên";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<DeliverySession>> call, Throwable t) {
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
        intent.putExtra("SESSION_STATUS", activeSessionStatus);
        intent.putExtra("HAS_UNFINISHED_TASKS", hasUnfinishedTasks());

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
    public void onUserStatusUpdate(String userId, boolean isOnline) {
        // User status updates are not relevant for TaskFragment
    }

    @Override
    public void onTypingIndicatorUpdate(String userId, String conversationId, boolean isTyping) {
        // Typing indicators are not relevant for TaskFragment
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
                
                // Show notification
                String sessionMessage;
                if (action == UpdateNotification.ActionType.COMPLETED) {
                    sessionMessage = "Phiên giao hàng đã kết thúc";
                } else {
                    sessionMessage = "Phiên giao hàng đã bị hủy";
                }
                showLightNotification(sessionMessage);
                
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
                
                // Show light notification (Snackbar)
                String message;
                if (action == UpdateNotification.ActionType.COMPLETED) {
                    message = "Đơn hàng đã hoàn thành";
                } else {
                    message = "Cập nhật đơn hàng";
                }
                showLightNotification(message);
                
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
    
    /**
     * Show light notification (Snackbar) when update notification is received
     */
    private void showLightNotification(String message) {
        if (getView() != null && message != null && !message.isEmpty()) {
            Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT);
            snackbar.setAction("Làm mới", v -> {
                resetAndFetchTasks();
            });
            snackbar.show();
        }
    }
    
    /**
     * Update local task status in memory immediately (optimistic update)
     * This ensures UI reflects the change immediately while API call is in progress
     */
    public void updateLocalTaskStatus(String parcelId, String newStatus) {
        if (parcelId == null || tasks == null || adapter == null) {
            return;
        }
        
        // Find and update task in local list
        for (DeliveryAssignment task : tasks) {
            if (parcelId.equals(task.getParcelId())) {
                task.setStatus(newStatus);
                // Update completedAt if status is COMPLETED
                if ("COMPLETED".equals(newStatus)) {
                    task.setCompletedAt(java.time.LocalDateTime.now().toString());
                }
                // Notify adapter to refresh UI
                adapter.notifyDataSetChanged();
                Log.d(TAG, "✅ Updated local task status: parcelId=" + parcelId + ", newStatus=" + newStatus);
                break;
            }
        }
    }
}
