package com.ds.communication_service.app_context.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ds.communication_service.app_context.models.Ticket;
import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    
    /**
     * Find tickets by parcel ID
     */
    List<Ticket> findByParcelId(String parcelId);
    
    /**
     * Find tickets by assignment ID
     */
    List<Ticket> findByAssignmentId(String assignmentId);
    
    /**
     * Find tickets by reporter ID
     */
    Page<Ticket> findByReporterId(String reporterId, Pageable pageable);
    
    /**
     * Find tickets by assigned admin ID
     */
    Page<Ticket> findByAssignedAdminId(String assignedAdminId, Pageable pageable);
    
    /**
     * Find tickets by status
     */
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
    
    /**
     * Find tickets by type
     */
    Page<Ticket> findByType(TicketType type, Pageable pageable);
    
    /**
     * Find tickets by status and type
     */
    Page<Ticket> findByStatusAndType(TicketStatus status, TicketType type, Pageable pageable);
    
    /**
     * Find open tickets (not resolved or cancelled)
     */
    @Query("SELECT t FROM Ticket t WHERE t.status NOT IN ('RESOLVED', 'CANCELLED')")
    Page<Ticket> findOpenTickets(Pageable pageable);
    
    /**
     * Count tickets by status
     */
    long countByStatus(TicketStatus status);
    
    /**
     * Find tickets by parcel ID and status
     */
    List<Ticket> findByParcelIdAndStatus(String parcelId, TicketStatus status);
    
    /**
     * Find tickets by list of IDs (bulk query)
     */
    List<Ticket> findByIdIn(List<UUID> ids);
    
    /**
     * Find tickets by list of parcel IDs (bulk query)
     */
    List<Ticket> findByParcelIdIn(List<String> parcelIds);
    
    /**
     * Find tickets by list of assignment IDs (bulk query)
     */
    List<Ticket> findByAssignmentIdIn(List<String> assignmentIds);
    
    /**
     * Find tickets by list of reporter IDs (bulk query)
     */
    List<Ticket> findByReporterIdIn(List<String> reporterIds);
}
