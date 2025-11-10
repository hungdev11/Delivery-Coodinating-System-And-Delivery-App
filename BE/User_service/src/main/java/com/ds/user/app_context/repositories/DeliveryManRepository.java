package com.ds.user.app_context.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ds.user.common.entities.base.DeliveryMan;

public interface DeliveryManRepository extends JpaRepository<DeliveryMan, UUID>, JpaSpecificationExecutor<DeliveryMan> {
    Optional<DeliveryMan> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
    
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT dm FROM DeliveryMan dm WHERE dm.id = :id")
    Optional<DeliveryMan> findByIdWithUser(@Param("id") UUID id);
    
    @EntityGraph(attributePaths = {"user"})
    @Override
    Page<DeliveryMan> findAll(Specification<DeliveryMan> spec, Pageable pageable);
}
