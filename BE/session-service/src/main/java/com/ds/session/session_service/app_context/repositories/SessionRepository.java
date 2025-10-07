package com.ds.session.session_service.app_context.repositories;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ds.session.session_service.app_context.models.Session;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID>{

    Optional<Session> findByTask_IdAndDeliveryManId(UUID taskId, String deliveryManId);

    Page<Session> findByDeliveryManIdAndAssignedAtBetween(String deliveryManId, LocalDateTime beginTime,
            LocalDateTime endTime, Pageable pageable);

}
