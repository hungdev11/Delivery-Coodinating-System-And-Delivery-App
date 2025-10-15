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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ActivityFragment extends Fragment implements TasksAdapter.OnTaskClickListener {

    private RecyclerView rvActivities;
    private TasksAdapter adapter;
    private List<DeliveryAssignment> completedTasks;

    // Đã thay thế tvSelectedDate bằng hai TextView
    private TextView tvStartDate, tvEndDate, tvDistance, tvOrdersCount, tvTime, tvEmptyState;
    private Button btnSelectDates;

    // Biến cho khoảng thời gian
    private LocalDate selectedStartDate;
    private LocalDate selectedEndDate;

    private final DateTimeFormatter apiFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter uiFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String DRIVER_ID = "0bbfa6a6-1c0b-4e4f-9e6e-11e36c142ea5";
    private static final String TAG = "HistoryFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        completedTasks = new ArrayList<>();
        adapter = new TasksAdapter(completedTasks, this);

        // 1. Ánh xạ Views
        initViews(view);

        // 2. Thiết lập RecyclerView
        rvActivities.setLayoutManager(new LinearLayoutManager(getContext()));
        rvActivities.setAdapter(adapter);

        // 3. Thiết lập khoảng ngày mặc định (Hôm nay)
        selectedStartDate = LocalDate.now();
        selectedEndDate = LocalDate.now();
        updateDateDisplay();
        fetchTasksByDateRange(selectedStartDate, selectedEndDate);

        // 4. Thiết lập sự kiện chọn ngày
        btnSelectDates.setOnClickListener(v -> showDatePickerDialog(true));

        return view;
    }

    private void initViews(View view) {
        // Ánh xạ các View mới cho ngày bắt đầu và kết thúc
        tvStartDate = view.findViewById(R.id.tv_start_date);
        tvEndDate = view.findViewById(R.id.tv_end_date);
        btnSelectDates = view.findViewById(R.id.btn_select_dates);

        tvDistance = view.findViewById(R.id.tvDistance);
        tvOrdersCount = view.findViewById(R.id.tvOrdersCount);
        tvTime = view.findViewById(R.id.tvTime);
        rvActivities = view.findViewById(R.id.recyclerActivities);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
    }

    private void updateDateDisplay() {
        tvStartDate.setText(selectedStartDate.format(uiFormatter));
        tvEndDate.setText(selectedEndDate.format(uiFormatter));
    }

    /**
     * Hiển thị DatePickerDialog, gọi đệ quy để chọn ngày bắt đầu và ngày kết thúc.
     * @param isSelectingStartDate True nếu đang chọn ngày bắt đầu, False nếu đang chọn ngày kết thúc.
     */
    private void showDatePickerDialog(boolean isSelectingStartDate) {
        LocalDate initialDate = isSelectingStartDate ? selectedStartDate : selectedEndDate;
        String title = isSelectingStartDate ? "Chọn Ngày Bắt Đầu" : "Chọn Ngày Kết Thúc";

        DatePickerDialog dialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    LocalDate chosenDate = LocalDate.of(year, month + 1, dayOfMonth);

                    if (isSelectingStartDate) {
                        selectedStartDate = chosenDate;
                        // Sau khi chọn ngày bắt đầu, chuyển sang chọn ngày kết thúc
                        // Đảm bảo ngày kết thúc tối thiểu bằng ngày bắt đầu
                        if (selectedEndDate.isBefore(selectedStartDate)) {
                            selectedEndDate = selectedStartDate;
                        }
                        showDatePickerDialog(false); // Gọi lại để chọn ngày kết thúc
                    } else {
                        // Đang chọn ngày kết thúc
                        if (chosenDate.isBefore(selectedStartDate)) {
                            Toast.makeText(getContext(), "Ngày kết thúc không được trước ngày bắt đầu.", Toast.LENGTH_LONG).show();
                            showDatePickerDialog(false); // Mở lại dialog để chọn lại ngày kết thúc
                            return;
                        }
                        selectedEndDate = chosenDate;

                        updateDateDisplay();
                        fetchTasksByDateRange(selectedStartDate, selectedEndDate);
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

    public void fetchTasksByDateRange(LocalDate startDate, LocalDate endDate) {
        tvEmptyState.setVisibility(View.GONE);

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        SessionClient service = retrofit.create(SessionClient.class);

        String start = startDate.format(apiFormatter);
        String end = endDate.format(apiFormatter);

        // Gọi API với hai tham số start và end
        Call<List<DeliveryAssignment>> call = service.getTasksIn(DRIVER_ID, start, end);

        call.enqueue(new Callback<List<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<List<DeliveryAssignment>> call, Response<List<DeliveryAssignment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<DeliveryAssignment> newTasks = response.body();

                    completedTasks.clear();
                    // Lọc chỉ giữ lại các đơn đã xử lý (hoàn thành/thất bại)
                    for (DeliveryAssignment task : newTasks) {
                        if ("COMPLETED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
                            completedTasks.add(task);
                        }
                    }

                    adapter.updateTasks(completedTasks);
                    calculateSummary(completedTasks);

                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.code());
                    Toast.makeText(getContext(), "Lỗi tải lịch sử đơn hàng: " + response.code(), Toast.LENGTH_SHORT).show();
                    calculateSummary(new ArrayList<>());
                }
            }
            @Override
            public void onFailure(Call<List<DeliveryAssignment>> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối mạng.", Toast.LENGTH_LONG).show();
                calculateSummary(new ArrayList<>());
            }
        });
    }

    private void calculateSummary(List<DeliveryAssignment> tasks) {
        double totalDistanceM = 0;
        long totalDurationS = 0;
        int completedCount = 0;

        for (DeliveryAssignment task : tasks) {
            totalDistanceM += task.getRouteDistanceM();
            totalDurationS += task.getRouteDurationS();
            completedCount++;
        }

        if (completedCount == 0) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }

        // Hiển thị tổng hợp
        tvDistance.setText("Tổng quãng đường: " + formatDistanceM(totalDistanceM));
        tvOrdersCount.setText("Tổng đơn đã xử lý: " + completedCount);
        tvTime.setText("Tổng thời gian: " + formatDurationS(totalDurationS));
    }
    // Triển khai phương thức click
    @Override
    public void onOrderClick(DeliveryAssignment task) {
        Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
        intent.putExtra("TASK_DETAIL", task);
        startActivity(intent);
    }
}
