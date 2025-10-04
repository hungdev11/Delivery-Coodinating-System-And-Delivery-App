package com.ds.session.session_service.app_context.repositories;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ds.session.session_service.app_context.models.Session;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID>{

}
