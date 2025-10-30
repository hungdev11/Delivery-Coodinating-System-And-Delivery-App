package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.deliveryapp.adapter.TasksAdapter;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.utils.TaskActionHandler;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.Serializable;
import java.util.List;

public class TaskListDialogFragment extends BottomSheetDialogFragment implements TasksAdapter.OnTaskClickListener, TaskActionHandler.TaskUpdateListener {

    private static final String ARG_ASSIGNMENTS = "assignments_list";
    private RecyclerView rvTaskList;
    private List<DeliveryAssignment> assignments;
    private TextView tvSelectedTaskAddress;
    private Button btnCompleteTask;
    private Button btnReportIssue;
    private DeliveryAssignment selectedAssignment;
    private TasksAdapter adapter;
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

        rvTaskList = view.findViewById(R.id.rv_task_list);
        tvSelectedTaskAddress = view.findViewById(R.id.tv_selected_task_address);
        btnCompleteTask = view.findViewById(R.id.btn_complete_task);
        btnReportIssue = view.findViewById(R.id.btn_report_issue);

        if (getActivity() != null) {
            // 💡 SỬA: Truyền 'this' (Fragment) thay vì 'getActivity()'
            // Điều này đảm bảo TaskActionHandler gọi requestPermissions trên Fragment
            actionHandler = new TaskActionHandler(this, this);
        }

        adapter = new TasksAdapter(assignments, this);
        rvTaskList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTaskList.setAdapter(adapter);

        if (assignments != null && !assignments.isEmpty()) {
            displayTaskDetails(assignments.get(0));
        }

        btnCompleteTask.setOnClickListener(v -> {
            if (selectedAssignment != null) {
                actionHandler.startCompletionFlow(selectedAssignment);
            }
        });

        btnReportIssue.setOnClickListener(v -> {
            if (selectedAssignment != null) {
                actionHandler.startFailureFlow(selectedAssignment);
            } else {
                Toast.makeText(getContext(), "Vui lòng chọn đơn hàng cần báo cáo.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void displayTaskDetails(DeliveryAssignment assignment) {
        selectedAssignment = assignment;
        tvSelectedTaskAddress.setText("Địa chỉ: " + assignment.getDeliveryLocation() + " " + assignment.getParcelId());

        if (assignment.getStatus().equals("IN_PROGRESS")) {
            btnCompleteTask.setEnabled(true);
            btnReportIssue.setEnabled(true);
        } else {
            btnCompleteTask.setEnabled(false);
            btnReportIssue.setEnabled(false);
        }
    }

    @Override
    public void onTaskClick(DeliveryAssignment task) {
//        // 1. Cập nhật task đang chọn trong dialog (để các nút Hoàn tất/Thất bại hoạt động)
//        displayTaskDetails(task);
//
//        // 2. Lấy MapFragment (là parent) và yêu cầu nó vẽ lại đường đi cho task này
//        Fragment parent = getParentFragment();
//        if (parent instanceof MapFragment) {
//            ((MapFragment) parent).displayTaskAndRoute(task);
//        }
//
//        // 3. (BỎ) Không mở TaskDetailActivity nữa, vì logic đã xử lý tại chỗ
         Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
         intent.putExtra("TASK_DETAIL", task);
         startActivity(intent);
    }

    @Override
    public void onStatusUpdated(String newStatus) {
        if (selectedAssignment != null) {
            selectedAssignment.setStatus(newStatus);
            adapter.notifyDataSetChanged();

            if (newStatus.equals("COMPLETED") || newStatus.equals("FAILED")) {
                Toast.makeText(getContext(), "Cập nhật thành công: " + newStatus, Toast.LENGTH_SHORT).show();
                dismiss();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("DialogFragment", "onActivityResult - Request Code: " + requestCode + ", Result Code: " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (actionHandler != null) {
            actionHandler.handleActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (actionHandler != null) {
            actionHandler.handlePermissionResult(requestCode, permissions, grantResults);
        }
    }
}