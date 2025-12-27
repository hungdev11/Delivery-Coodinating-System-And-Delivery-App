package com.ds.user.app_context.repositories;

import com.ds.user.common.entities.base.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, String>, JpaSpecificationExecutor<UserAddress> {
    
    /**
     * Find all addresses for a user
     */
    List<UserAddress> findByUserId(String userId);
    
    /**
     * Find primary address for a user
     */
    Optional<UserAddress> findByUserIdAndIsPrimaryTrue(String userId);
    
    /**
     * Find address by user ID and address ID
     */
    Optional<UserAddress> findByIdAndUserId(String id, String userId);
    
    /**
     * Count addresses for a user
     */
    long countByUserId(String userId);
    
    /**
     * Set all addresses for a user as non-primary
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserAddress ua SET ua.isPrimary = false WHERE ua.userId = :userId")
    void setAllNonPrimaryByUserId(@Param("userId") String userId);
    
    /**
     * Check if user has any addresses
     */
    boolean existsByUserId(String userId);
}
