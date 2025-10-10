package com.ds.session.session_service.business.v1.services;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.session.session_service.app_context.models.Task;
import com.ds.session.session_service.app_context.repositories.TaskRepository;
import com.ds.session.session_service.business.v1.services.ParcelMock.Parcel;
import com.ds.session.session_service.common.entities.dto.request.CreateTaskRequest;
import com.ds.session.session_service.common.entities.dto.request.UpdateTaskRequest;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.entities.dto.response.TaskResponse;
import com.ds.session.session_service.common.enums.TaskStatus;
import com.ds.session.session_service.common.exceptions.ResourceNotFound;
import com.ds.session.session_service.common.interfaces.ITaskService;
import com.ds.session.session_service.common.utils.PageUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskService implements ITaskService {

    private final TaskRepository taskRepository;

    @Override
    public TaskResponse createTask(CreateTaskRequest request) {
        ParcelMock.Parcel parcel = getParcel(request.getParcelId());
        validateParcelInfo(parcel);

        Task newTask = Task.builder()
                .parcelId(request.getParcelId())
                .status(TaskStatus.CREATED)
                .build();

        Task savedTask = taskRepository.save(newTask);
        return toDto(savedTask, parcel);
    }

    @Override
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFound("Task not found: " + taskId));

        if (request.getStatus() != null) {
            task.setStatus(TaskStatus.valueOf(request.getStatus()));
        }

        if (request.getCompletedAt() != null) {
            task.setCompletedAt(request.getCompletedAt());
        }

        taskRepository.save(task);

        ParcelMock.Parcel parcel = getParcel(task.getParcelId());
        return toDto(task, parcel);
    }

    @Override
    public void deleteTask(UUID id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFound("Task not found: " + id);
        }
        taskRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getTasks(int page, int size, String sortBy, String direction) {
        Pageable pageable = PageUtil.build(page, size, sortBy, direction, Task.class);
        Page<Task> tasks = taskRepository.findAll(pageable);

        return PageResponse.from(tasks.map(t -> {
            return toDto(t, getParcel(t.getParcelId()));
        }));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaskResponse> getTask(UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Task not found: " + id));
        ParcelMock.Parcel parcel = getParcel(task.getParcelId());
        return Optional.of(toDto(task, parcel));
    }

    // =======================
    private Parcel getParcel(String parcelId) {
        return ParcelMock.getParcels().stream()
                .filter(p -> p.parcelId.equals(parcelId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFound("Parcel not found: " + parcelId));
    }

    private void validateParcelInfo(ParcelMock.Parcel parcel) {
        if (parcel == null) {
            throw new ResourceNotFound("Parcel info not found");
        }
    }

    private TaskResponse toDto(Task task, ParcelMock.Parcel parcel) {
        return TaskResponse.builder()
                .taskId(task.getId().toString())
                .parcelId(task.getParcelId())
                .attemptCount(task.getAttemptCount())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .status(task.getStatus().toString())
                .receiverName(parcel.receiverName)
                .receiverPhone(parcel.receiverPhone)
                .weight(parcel.weight)
                .note(parcel.note)
                .deliveryLocation(parcel.deliveryLocation.toString())
                .build();
    }
}
