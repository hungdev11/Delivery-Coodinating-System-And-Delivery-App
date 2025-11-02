package com.ds.communication_service.app_context.models;

import java.sql.Types;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import com.ds.communication_service.common.enums.ProposalActionType;
import com.ds.communication_service.common.enums.ProposalType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "proposal_type_configs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"type"})
})
public class ProposalTypeConfig {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, unique = true, length = 100)
    private ProposalType type;

    @Column(name = "required_role", nullable = false, length = 50)
    private String requiredRole;

    @Column(nullable = false)
    private String description;

    @Column(nullable = true)
    private Long defaultTimeoutMinutes;

    // UI cho người TẠO (Sender) ---
    @Enumerated(EnumType.STRING)
    @Column(name = "creation_action_type", nullable = false, length = 50)
    private ProposalActionType creationActionType;

    // UI cho người PHẢN HỒI (Receiver) ---
    @Enumerated(EnumType.STRING)
    @Column(name = "response_action_type", nullable = false, length = 50)
    private ProposalActionType responseActionType;
}
