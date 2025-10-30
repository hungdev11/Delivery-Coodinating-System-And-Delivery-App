package com.ds.communication_service.app_context.models;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Table(name="conversations", uniqueConstraints=
    @UniqueConstraint(columnNames={"user1_id", "user2_id"})) 
@Check(constraints = "user1_id < user2_id")
@Getter 
@Setter
public class Conversation {

    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name="user1_id", nullable=false)
    private String user1Id;

    @Column(name="user2_id", nullable=false)
    private String user2Id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "last_message_id")
    private Message lastMessage;
}