package com.ds.session.session_service.common.interfaces;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ds.session.session_service.common.entities.dto.request.CreateTaskRequest;
import com.ds.session.session_service.common.entities.dto.request.UpdateTaskRequest;
import com.ds.session.session_service.common.entities.dto.response.TaskResponse;

public interface ITaskService {
    TaskResponse createTask(CreateTaskRequest request);
    TaskResponse updateTask(UpdateTaskRequest request);
    void deleteTask(UUID id);
    List<TaskResponse> getTasks();
    Optional<TaskResponse> getTask(UUID id);
}
