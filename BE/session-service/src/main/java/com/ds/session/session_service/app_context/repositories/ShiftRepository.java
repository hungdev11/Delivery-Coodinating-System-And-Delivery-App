package com.ds.session.session_service.app_context.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ds.session.session_service.app_context.models.Shift;
import com.ds.session.session_service.common.enums.ShiftType;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    Optional<Shift> findByType(ShiftType type);
    boolean existsByType(ShiftType type);
}
