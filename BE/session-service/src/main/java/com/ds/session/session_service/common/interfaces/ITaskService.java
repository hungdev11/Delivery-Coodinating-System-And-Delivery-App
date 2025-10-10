package com.ds.session.session_service.common.interfaces;

import java.util.Optional;
import java.util.UUID;

import com.ds.session.session_service.common.entities.dto.request.CreateTaskRequest;
import com.ds.session.session_service.common.entities.dto.request.UpdateTaskRequest;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.entities.dto.response.TaskResponse;

public interface ITaskService {
    TaskResponse createTask(CreateTaskRequest request);
    TaskResponse updateTask(UUID taskId, UpdateTaskRequest request);
    void deleteTask(UUID id);
    PageResponse<TaskResponse> getTasks(int page, int size, String sortBy, String direction);
    Optional<TaskResponse> getTask(UUID id);
}
