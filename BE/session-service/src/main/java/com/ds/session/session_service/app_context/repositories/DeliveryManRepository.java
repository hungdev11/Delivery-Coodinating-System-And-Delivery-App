package com.ds.session.session_service.app_context.repositories;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ds.session.session_service.app_context.models.DeliveryMan;

@Repository
public interface DeliveryManRepository extends JpaRepository<DeliveryMan, UUID>{

}
