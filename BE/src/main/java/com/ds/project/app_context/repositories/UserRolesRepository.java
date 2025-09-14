package com.ds.project.app_context.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ds.project.app_context.models.Role;
import com.ds.project.app_context.models.User;
import com.ds.project.app_context.models.UserRoles;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserRoles entity
 */
@Repository
public interface UserRolesRepository extends JpaRepository<UserRoles, String> {
    
    /**
     * Find all user roles by user ID
     */
    List<UserRoles> findByUserId(String userId);
    
    /**
     * Find all user roles by role ID
     */
    List<UserRoles> findByRoleId(String roleId);
    
    /**
     * Delete user roles by user ID
     */
    void deleteByUserId(String userId);
    
    /**
     * Delete user roles by role ID
     */
    void deleteByRoleId(String roleId);

    /**
     * Find user role by user and role
     */
    Optional<UserRoles> findByUserAndRole(User user, Role role);
    
    /**
     * Check if user role exists by user and role
     */
    boolean existsByUserAndRole(User user, Role role);
}
