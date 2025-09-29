package com.ds.app_context.repositories;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ds.app_context.models.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByPhone(String phone);
    Optional<User> findByPhone(String phone);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
