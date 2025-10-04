package com.ds.session.session_service.business.v1.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.session.session_service.app_context.models.Task;
import com.ds.session.session_service.app_context.repositories.TaskRepository;
import com.ds.session.session_service.common.entities.dto.request.CreateTaskRequest;
import com.ds.session.session_service.common.entities.dto.request.UpdateTaskRequest;
import com.ds.session.session_service.common.entities.dto.response.TaskResponse;
import com.ds.session.session_service.common.enums.TaskStatus;
import com.ds.session.session_service.common.exceptions.ResourceNotFound;
import com.ds.session.session_service.common.interfaces.ITaskService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService implements ITaskService {
    
    private final TaskRepository taskRepository;

    @Override
    public TaskResponse createTask(CreateTaskRequest request) {
        Object parcel = getParcel(request.getParcelId());
        validateParcelInfo(parcel);

        Task newTask = Task.builder()
                .parcelId(request.getParcelId())
                .status(TaskStatus.CREATED)
                .build();

        Task savedTask = taskRepository.save(newTask);
        return toDto(savedTask);
    }

    @Override
    public TaskResponse updateTask(UpdateTaskRequest request) {
        Task task = taskRepository.findById(UUID.fromString(request.getTaskId()))
                .orElseThrow(() -> new ResourceNotFound("Task not found: " + request.getTaskId()));

        if (request.getStatus() != null) {
            task.setStatus(TaskStatus.valueOf(request.getStatus()));
        }

        if (request.getCompletedAt() != null) {
            task.setCompletedAt(request.getCompletedAt());
        }

        taskRepository.save(task);
        return toDto(task);
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
    public List<TaskResponse> getTasks() {
        return taskRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaskResponse> getTask(UUID id) {
        return taskRepository.findById(id).map(this::toDto);
    }

    // =======================
    private Object getParcel(String parcelId) {
        // TODO: call parcel-service API and verify parcel existence
        return new Object();
    }

    private void validateParcelInfo(Object parcel) {
        // TODO: add validation logic for parcel (e.g., not delivered, correct status)
    }

    private TaskResponse toDto(Task task) {
        return TaskResponse.builder()
                .taskId(task.getId().toString())
                .parcelId(task.getParcelId())
                .attemptCount(task.getAttemptCount())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .status(task.getStatus().toString())
                .build();
    }
}
