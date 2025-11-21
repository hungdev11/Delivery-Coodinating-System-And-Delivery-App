package com.ds.communication_service.app_context.repositories;

import com.ds.communication_service.app_context.models.UserSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSnapshotRepository extends JpaRepository<UserSnapshot, String> {
    Optional<UserSnapshot> findByUserId(String userId);
    Optional<UserSnapshot> findByUsername(String username);
}
