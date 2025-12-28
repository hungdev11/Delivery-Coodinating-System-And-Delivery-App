package com.ds.communication_service.app_context.repositories;

import com.ds.communication_service.app_context.models.Ticket;
import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Ticket entity
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    /**
     * Find tickets by parcel ID
     */
    List<Ticket> findByParcelIdOrderByCreatedAtDesc(String parcelId);

    /**
     * Find tickets by delivery assignment ID
     */
    List<Ticket> findByDeliveryAssignmentIdOrderByCreatedAtDesc(String deliveryAssignmentId);

    /**
     * Find tickets by user ID (paginated)
     */
    Page<Ticket> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find tickets by status (paginated)
     */
    Page<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);

    /**
     * Find tickets by type (paginated)
     */
    Page<Ticket> findByTypeOrderByCreatedAtDesc(TicketType type, Pageable pageable);

    /**
     * Find tickets by status and type (paginated)
     */
    Page<Ticket> findByStatusAndTypeOrderByCreatedAtDesc(TicketStatus status, TicketType type, Pageable pageable);

    /**
     * Find all tickets (paginated) - for admin
     */
    Page<Ticket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Count tickets by status
     */
    long countByStatus(TicketStatus status);

    /**
     * Count tickets by user ID and status
     */
    long countByUserIdAndStatus(String userId, TicketStatus status);
}
