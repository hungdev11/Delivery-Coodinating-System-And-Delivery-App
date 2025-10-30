package com.ds.deliveryapp;

import static com.ds.deliveryapp.utils.FormaterUtil.formatDistanceM;
import static com.ds.deliveryapp.utils.FormaterUtil.formatDurationS;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.ds.deliveryapp.utils.SpinnerItem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter; // Import
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Màn hình Lịch sử (Activity).
 */
public class ActivityFragment extends Fragment implements TasksAdapter.OnTaskClickListener {

    private RecyclerView rvActivities;
    private TasksAdapter adapter;
    private List<DeliveryAssignment> allTasks = new ArrayList<>();

    private TextView tvDistance, tvOrdersCount, tvTime, tvEmptyState;
    private TextView tvCreatedAtStart, tvCreatedAtEnd;
    private ImageView imgCreatedAtPicker;
    private TextView tvCompletedAtStart, tvCompletedAtEnd;
    private ImageView imgCompletedAtPicker;
    private Spinner spinnerStatus;
    private Button btnApplyFilters;

    private LocalDate createdAtStart;
    private LocalDate createdAtEnd;
    private LocalDate completedAtStart;
    private LocalDate completedAtEnd;

    private int currentPage = 0;
    private final int pageSize = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private List<SpinnerItem> statusOptions;

    private LinearLayoutManager layoutManager;

    //Định dạng API phải là ISO (yyyy-MM-dd) để khớp với Specification
    private final DateTimeFormatter apiFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private final DateTimeFormatter uiFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String DRIVER_ID = "0bbfa6a6-1c0b-4e4f-9e6e-11e36c142ea5";
    private static final String TAG = "ActivityFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        adapter = new TasksAdapter(allTasks, this);

        // 1. Ánh xạ Views
        initViews(view);

        // 2. Thiết lập RecyclerView
        layoutManager = new LinearLayoutManager(getContext());
        rvActivities.setLayoutManager(layoutManager);
        rvActivities.setAdapter(adapter);
        setupPaginationScrollListener();

        // 3. Thiết lập Filter và Spinner
        setDefaultFilters();
        setupStatusSpinnerLogic(); // Sử dụng logic SpinnerItem

        // 4. Thiết lập sự kiện
        imgCreatedAtPicker.setOnClickListener(v -> showDatePickerDialog(true, true));
        imgCompletedAtPicker.setOnClickListener(v -> showDatePickerDialog(false, true));
        btnApplyFilters.setOnClickListener(v -> {
            resetPaginationAndFetchTasks();
        });

        // Tải dữ liệu lần đầu
        fetchTasks(currentPage);

        return view;
    }

    private void initViews(View view) {
        tvDistance = view.findViewById(R.id.tvDistance);
        tvOrdersCount = view.findViewById(R.id.tvOrdersCount);
        tvTime = view.findViewById(R.id.tvTime);
        rvActivities = view.findViewById(R.id.recyclerActivities);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        tvCreatedAtStart = view.findViewById(R.id.tv_created_at_start);
        tvCreatedAtEnd = view.findViewById(R.id.tv_created_at_end);
        imgCreatedAtPicker = view.findViewById(R.id.img_created_at_picker);
        tvCompletedAtStart = view.findViewById(R.id.tv_completed_at_start);
        tvCompletedAtEnd = view.findViewById(R.id.tv_completed_at_end);
        imgCompletedAtPicker = view.findViewById(R.id.img_completed_at_picker);
        spinnerStatus = view.findViewById(R.id.spinner_status);
        btnApplyFilters = view.findViewById(R.id.btn_apply_filters);
    }

    private void initializeStatusOptions() {
        statusOptions = new ArrayList<>();
        statusOptions.add(new SpinnerItem("Tất cả", null));
        statusOptions.add(new SpinnerItem("Đang giao", "IN_PROGRESS"));
        // SỬA LỖI 2: "SUCCESS" -> "COMPLETED"
        statusOptions.add(new SpinnerItem("Đã giao thành công", "COMPLETED"));
        statusOptions.add(new SpinnerItem("Giao hàng thất bại", "FAILED"));
    }

    // (setupStatusSpinnerLogic, getSelectedStatuses, setDefaultFilters,
    //  updateDateDisplay, showDatePickerDialog, resetPaginationAndFetchTasks
    //  giữ nguyên như file gốc)

    private void setupStatusSpinnerLogic() {
        initializeStatusOptions();
        ArrayAdapter<SpinnerItem> adapter = new ArrayAdapter<>(
                getContext(),
                R.layout.spinner_item,
                statusOptions
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
        spinnerStatus.setSelection(0);
    }

    private List<String> getSelectedStatuses() {
        SpinnerItem selectedItem = (SpinnerItem) spinnerStatus.getSelectedItem();
        String apiValue = selectedItem.getApiValue();
        if (apiValue == null) return null;
        return Arrays.asList(apiValue);
    }

    private void setDefaultFilters() {
        createdAtStart = LocalDate.now();
        createdAtEnd = LocalDate.now();
        completedAtStart = null;
        completedAtEnd = null;
        updateDateDisplay(true);
        updateDateDisplay(false);
    }

    private void updateDateDisplay(boolean isCreatedAt) {
        if (isCreatedAt) {
            tvCreatedAtStart.setText(createdAtStart != null ? createdAtStart.format(uiFormatter) : "Chọn ngày");
            tvCreatedAtEnd.setText(createdAtEnd != null ? createdAtEnd.format(uiFormatter) : "Chọn ngày");
        } else {
            tvCompletedAtStart.setText(completedAtStart != null ? completedAtStart.format(uiFormatter) : "Không lọc");
            tvCompletedAtEnd.setText(completedAtEnd != null ? completedAtEnd.format(uiFormatter) : "Không lọc");
        }
    }

    private void showDatePickerDialog(boolean isCreatedDate, boolean isSelectingStartDate) {
        // (Logic chọn ngày giữ nguyên)
        LocalDate currentStart = isCreatedDate ? createdAtStart : completedAtStart;
        LocalDate currentEnd = isCreatedDate ? createdAtEnd : completedAtEnd;

        LocalDate initialDate = isSelectingStartDate ? currentStart : currentEnd;
        String title = isSelectingStartDate ? "Chọn Ngày Bắt Đầu" : "Chọn Ngày Kết Thúc";
        if (initialDate == null) initialDate = LocalDate.now();

        DatePickerDialog dialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    LocalDate chosenDate = LocalDate.of(year, month + 1, dayOfMonth);

                    if (isSelectingStartDate) {
                        if (isCreatedDate) {
                            createdAtStart = chosenDate;
                            if (createdAtEnd != null && createdAtEnd.isBefore(createdAtStart)) {
                                createdAtEnd = createdAtStart;
                            }
                        } else {
                            completedAtStart = chosenDate;
                            if (completedAtEnd != null && completedAtEnd.isBefore(completedAtStart)) {
                                completedAtEnd = completedAtStart;
                            }
                        }
                        // Hiển thị dialog chọn ngày kết thúc
                        showDatePickerDialog(isCreatedDate, false);
                    } else {
                        LocalDate finalStart = isCreatedDate ? createdAtStart : completedAtStart;
                        // Đảm bảo ngày kết thúc không trước ngày bắt đầu
                        if (finalStart != null && chosenDate.isBefore(finalStart)) {
                            Toast.makeText(getContext(), "Ngày kết thúc không được trước ngày bắt đầu.", Toast.LENGTH_LONG).show();
                            // Hiển thị lại dialog chọn ngày kết thúc
                            showDatePickerDialog(isCreatedDate, false);
                            return;
                        }

                        if (isCreatedDate) {
                            createdAtEnd = chosenDate;
                        } else {
                            completedAtEnd = chosenDate;
                        }
                        // Cập nhật UI sau khi chọn xong
                        updateDateDisplay(isCreatedDate);
                    }
                },
                initialDate.getYear(),
                initialDate.getMonthValue() - 1,
                initialDate.getDayOfMonth()
        );

        dialog.setTitle(title);
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void resetPaginationAndFetchTasks() {
        currentPage = 0;
        isLastPage = false;
        allTasks.clear();
        adapter.notifyDataSetChanged();
        //calculateSummary(new ArrayList<>());
        fetchTasks(currentPage);
    }


    public void fetchTasks(int page) {
        if (isLoading || isLastPage) return;

        isLoading = true;
        tvEmptyState.setVisibility(View.GONE);
        // ... (Hiển thị loading)

        Retrofit retrofit = RetrofitClient.getRetrofitInstance(getContext());
        SessionClient service = retrofit.create(SessionClient.class);

        // SỬA LỖI 1: Dùng apiFormatter (yyyy-MM-dd)
        String createdStartStr = createdAtStart != null ? createdAtStart.format(apiFormatter) : null;
        String createdEndStr = createdAtEnd != null ? createdAtEnd.format(apiFormatter) : null;
        String completedStartStr = completedAtStart != null ? completedAtStart.format(apiFormatter) : null;
        String completedEndStr = completedAtEnd != null ? completedAtEnd.format(apiFormatter) : null;
        List<String> statuses = getSelectedStatuses();

        Call<PageResponse<DeliveryAssignment>> call = service.getTasks(
                DRIVER_ID,
                statuses,
                createdStartStr,
                createdEndStr,
                completedStartStr,
                completedEndStr,
                page,
                pageSize
        );

        call.enqueue(new Callback<PageResponse<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<PageResponse<DeliveryAssignment>> call, Response<PageResponse<DeliveryAssignment>> response) {
                isLoading = false;
                // ... (Ẩn loading)

                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<DeliveryAssignment> pageResponse = response.body();
                    List<DeliveryAssignment> newTasks = pageResponse.content();

                    isLastPage = pageResponse.last();

                    if (page == 0) {
                        allTasks.clear();
                        // Chỉ tính summary cho trang đầu tiên (hoặc tính tổng sau)
                        // calculateSummary(newTasks);
                    }

                    allTasks.addAll(newTasks);
                    adapter.notifyDataSetChanged();

                    if (allTasks.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                    }

                    currentPage++;

                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.code() + " - " + response.errorBody());
                }
            }
            @Override
            public void onFailure(Call<PageResponse<DeliveryAssignment>> call, Throwable t) {
                isLoading = false;
                Log.e(TAG, "Network error: " + t.getMessage());
            }
        });
    }

    // (setupPaginationScrollListener, calculateSummary, onTaskClick
    //  giữ nguyên như file gốc)

    private void setupPaginationScrollListener() {
        rvActivities.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                        fetchTasks(currentPage);
                    }
                }
            }
        });
    }

    @Override
    public void onTaskClick(DeliveryAssignment task) {
        Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
        intent.putExtra("TASK_DETAIL", task);
        startActivity(intent);
    }
}
