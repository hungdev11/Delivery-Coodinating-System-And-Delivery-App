package com.ds.session.session_service.app_context.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ds.session.session_service.app_context.models.DeliveryProof;
import com.ds.session.session_service.common.enums.ProofType;

public interface DeliveryProofRepository extends JpaRepository<DeliveryProof, UUID>{
    
    /**
     * Find proofs by assignment parcel ID
     */
    List<DeliveryProof> findByAssignmentParcelId(UUID assignmentParcelId);
    
    /**
     * Find proofs by assignment ID (via JOIN through assignmentParcel)
     */
    @Query("SELECT p FROM DeliveryProof p JOIN p.assignmentParcel ap WHERE ap.assignment.id = :assignmentId")
    List<DeliveryProof> findByAssignmentId(@Param("assignmentId") UUID assignmentId);

    /**
     * Find proofs by assignment ID and type (via JOIN through assignmentParcel)
     */
    @Query("SELECT p FROM DeliveryProof p JOIN p.assignmentParcel ap WHERE ap.assignment.id = :assignmentId AND p.type = :type")
    List<DeliveryProof> findByAssignmentIdAndType(
            @Param("assignmentId") UUID assignmentId,
            @Param("type") ProofType type
    );
    
    /**
     * Find proofs by parcel ID (via JOIN through assignmentParcel)
     */
    @Query("SELECT p FROM DeliveryProof p JOIN p.assignmentParcel ap WHERE ap.parcelId = :parcelId")
    List<DeliveryProof> findByParcelId(@Param("parcelId") String parcelId);
}
