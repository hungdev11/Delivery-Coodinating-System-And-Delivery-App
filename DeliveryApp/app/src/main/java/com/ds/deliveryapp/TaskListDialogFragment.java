package com.ds.deliveryapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.deliveryapp.adapter.TasksAdapter;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.utils.TaskActionHandler; // 💡 Import Handler mới
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.Serializable;
import java.util.List;

// 💡 TRIỂN KHAI INTERFACE TaskUpdateListener
public class TaskListDialogFragment extends BottomSheetDialogFragment implements TasksAdapter.OnTaskClickListener, TaskActionHandler.TaskUpdateListener {

    private static final String ARG_ASSIGNMENTS = "assignments_list";
    private RecyclerView rvTaskList;
    private List<DeliveryAssignment> assignments;
    private TextView tvSelectedTaskAddress;
    private Button btnCompleteTask;
    private Button btnReportIssue;
    private DeliveryAssignment selectedAssignment;
    private TasksAdapter adapter;

    // 💡 BIẾN HANDLER MỚI
    private TaskActionHandler actionHandler;

    public static TaskListDialogFragment newInstance(List<DeliveryAssignment> assignments) {
        TaskListDialogFragment fragment = new TaskListDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ASSIGNMENTS, (Serializable) assignments);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            assignments = (List<DeliveryAssignment>) getArguments().getSerializable(ARG_ASSIGNMENTS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_list_task, container, false);

        // Ánh xạ Views
        rvTaskList = view.findViewById(R.id.rv_task_list);
        tvSelectedTaskAddress = view.findViewById(R.id.tv_selected_task_address);
        btnCompleteTask = view.findViewById(R.id.btn_complete_task);
        btnReportIssue = view.findViewById(R.id.btn_report_issue);

        // 💡 KHỞI TẠO HANDLER
        if (getActivity() != null) {
            actionHandler = new TaskActionHandler(getActivity(), this);
        }

        // Thiết lập RecyclerView
        adapter = new TasksAdapter(assignments, this);
        rvTaskList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTaskList.setAdapter(adapter);

        // Hiển thị chi tiết Task đầu tiên (mặc định)
        if (assignments != null && !assignments.isEmpty()) {
            displayTaskDetails(assignments.get(0));
        }

        // 1. Nút HOÀN TẤT
        btnCompleteTask.setOnClickListener(v -> {
            if (selectedAssignment != null) {
                // 💡 GỌI HANDLER: Bắt đầu luồng hoàn thành (chụp ảnh/quét QR)
                actionHandler.startCompletionFlow(selectedAssignment);
                // Vì luồng hoàn thành cần chụp ảnh (onActivityResult), ta không dismiss ngay
            }
        });

        // 2. Nút BÁO CÁO
        btnReportIssue.setOnClickListener(v -> {
            if (selectedAssignment != null) {
                // 💡 GỌI HANDLER: Bắt đầu luồng thất bại (dialog chọn lý do)
                actionHandler.startFailureFlow(selectedAssignment);
                // Handler sẽ gọi dismiss() sau khi xử lý xong
            } else {
                Toast.makeText(getContext(), "Vui lòng chọn đơn hàng cần báo cáo.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void displayTaskDetails(DeliveryAssignment assignment) {
        selectedAssignment = assignment;
        tvSelectedTaskAddress.setText("Địa chỉ: " + assignment.getDeliveryLocation() + " " + assignment.getParcelId());
        // Cập nhật các thông tin chi tiết khác tại đây
    }

    @Override
    public void onTaskClick(DeliveryAssignment task) {
        Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
        intent.putExtra("TASK_DETAIL", task);
        startActivity(intent);
    }

    // 💡 TRIỂN KHAI PHƯƠNG THỨC LẮNG NGHE CẬP NHẬT
    @Override
    public void onStatusUpdated(String newStatus) {
        // Cập nhật trạng thái của selectedAssignment (nếu nó là FAILED/COMPLETED)
        if (selectedAssignment != null) {
            selectedAssignment.setStatus(newStatus);
            // 💡 Cập nhật UI trong Dialog (Ví dụ: Ẩn/Hiển thị nút, hoặc refresh list)
            adapter.notifyDataSetChanged();

            // Nếu task đã hoàn tất hoặc thất bại, dismiss Dialog
            if (newStatus.equals("COMPLETED") || newStatus.equals("FAILED")) {
                Toast.makeText(getContext(), "Cập nhật thành công: " + newStatus, Toast.LENGTH_SHORT).show();
                dismiss();
            }
        }
    }

    // 💡 CẦN XỬ LÝ onActivityResult nếu flow hoàn thành cần chụp ảnh/quét QR
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (actionHandler != null && selectedAssignment != null) {
            actionHandler.handleActivityResult(requestCode, resultCode, data, selectedAssignment);
        }
    }
}