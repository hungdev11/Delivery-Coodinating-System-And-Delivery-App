package com.ds.session.session_service.business.v1.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.session.session_service.app_context.models.Session;
import com.ds.session.session_service.app_context.models.Task;
import com.ds.session.session_service.app_context.repositories.SessionRepository;
import com.ds.session.session_service.app_context.repositories.TaskRepository;
import com.ds.session.session_service.business.v1.services.ParcelMock.Parcel;
import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.entities.dto.response.TaskSessionResponse;
import com.ds.session.session_service.common.enums.TaskStatus;
import com.ds.session.session_service.common.exceptions.ResourceNotFound;
import com.ds.session.session_service.common.interfaces.ISessionService;
import com.ds.session.session_service.common.utils.PageUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SessionService implements ISessionService {

    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;

    @Override
    public void acceptTask(UUID taskId, UUID deliveryManId) {
        /*
         * Scan already show parcel info only for appropriate delivery man has proper working zone
         * check task and parcel status for is it ready for assign
         * (optional) check delivery man current capacity
         * create new task to assign
         * create session related with task and driver
         */
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFound("Task not found"));

        if (!(TaskStatus.CREATED.equals(task.getStatus()) || TaskStatus.FAILED.equals(task.getStatus()))) {
            throw new IllegalStateException("Can not re-assign task is delivering, value " + task.getStatus());
        }

        Session session = Session.builder()
                .deliveryManId(deliveryManId.toString())
                .task(task)
                .assignedAt(LocalDateTime.now())
                .build();

        sessionRepository.save(session);

        task.setStatus(TaskStatus.ASSIGNED);
        taskRepository.save(task);
    }

    @Override
    public TaskSessionResponse completeTask(UUID taskId, UUID deliveryManId, RouteInfo routeInfo) {
        insertRouteInfo(taskId, deliveryManId, routeInfo);
        Session session = findSession(taskId, deliveryManId);
        session.setFailReason(null);
        Task task = session.getTask();
        task.setStatus(TaskStatus.DELIVERED);

        taskRepository.save(task);
        sessionRepository.save(session);

        return buildResponse(session.getTask(), session);
    }

    @Override
    public TaskSessionResponse deliveryFailed(UUID taskId, UUID deliveryManId, String reason, RouteInfo routeInfo) {
        insertRouteInfo(taskId, deliveryManId, routeInfo);
        Session session = findSession(taskId, deliveryManId);
        session.setFailReason(reason);
        Task task = session.getTask();
        task.setStatus(TaskStatus.FAILED);

        taskRepository.save(task);
        sessionRepository.save(session);
        
        return buildResponse(session.getTask(), session);
    }

    @Override
    public TaskSessionResponse changeTimeWindow(UUID taskId, UUID deliveryManId, LocalDateTime startTime, LocalDateTime endTime) {
        Session session = findSession(taskId, deliveryManId);

        session.setWindowStart(startTime.toLocalTime());
        session.setWindowEnd(endTime.toLocalTime());

        Task task = session.getTask();
        task.setStatus(TaskStatus.FAILED);

        taskRepository.save(task);
        sessionRepository.save(session);

        return buildResponse(session.getTask(), session);
    }

    @Override
    public PageResponse<TaskSessionResponse> getTasksOfDeliveryMan(UUID deliveryManId, LocalDateTime beginTime,
            LocalDateTime endTime, int page, int size, String sortBy, String direction) {
        Pageable pageable = PageUtil.build(page, size, sortBy, direction);

        Page<Session> sessionPage = sessionRepository
                .findByDeliveryManIdAndAssignedAtBetween(deliveryManId.toString(), beginTime, endTime, pageable);

        List<TaskSessionResponse> result = sessionPage.getContent().stream()
                .map(s -> buildResponse(s.getTask(), s))
                .collect(Collectors.toList());

        return PageResponse.from(PageUtil.toPage(result, pageable));
    }

    @Override
    public PageResponse<TaskSessionResponse> getTaskTodayOfDeliveryMan(UUID deliveryManId, int page,
            int size, String sortBy, String direction) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        return getTasksOfDeliveryMan(deliveryManId, startOfDay, endOfDay, page, size, sortBy, direction);
    }

    private void insertRouteInfo(UUID taskId, UUID deliveryManId, RouteInfo routeInfo) {
        // insert route info even success or fail
        Session session = findSession(taskId, deliveryManId);
        session.setDistanceM(routeInfo.getDistanceM());
        session.setDurationS(routeInfo.getDurationS());
        session.setWaypoints(toJson(routeInfo.getWaypoints()));

        sessionRepository.save(session);
    }

    private String toJson(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize route info", e);
        }
    }

    private Session findSession(UUID taskId, UUID deliveryManId) {
        return sessionRepository.findByTask_IdAndDeliveryManId(taskId, deliveryManId.toString())
                .orElseThrow(() -> new ResourceNotFound("Session not found"));
    }

    private Parcel getParcelRelated(Task task) {
        return ParcelMock.getParcels().stream()
                .filter(p -> p.parcelId.equals(task.getParcelId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFound("Parcel not found"));
    }

    private TaskSessionResponse buildResponse(Task task, Session session) {
        Parcel parcel = getParcelRelated(task);
        TaskSessionResponse response = TaskSessionResponse.from(task, parcel);
        response.setSessionsInfo(List.of(TaskSessionResponse.SessionInfo.from(session)));
        return response;
    }
}
