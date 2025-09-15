package knp.ptithcm.datn.user_module.modules.user.repositories;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import knp.ptithcm.datn.user_module.modules.user.entities.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    // Có thể thêm các phương thức custom nếu cần
    boolean existsByPhone(String phone);
    Optional<User> findByPhone(String phone);
} 
