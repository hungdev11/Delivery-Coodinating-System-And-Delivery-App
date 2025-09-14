package com.ds.project.business.v1.services;

import com.ds.project.app_context.models.Role;
import com.ds.project.app_context.models.User;
import com.ds.project.app_context.models.UserRoles;
import com.ds.project.app_context.repositories.RoleRepository;
import com.ds.project.app_context.repositories.UserRepository;
import com.ds.project.app_context.repositories.UserRolesRepository;
import com.ds.project.common.entities.base.BaseResponse;
import com.ds.project.common.entities.base.Page;
import com.ds.project.common.entities.base.PagedData;
import com.ds.project.common.entities.dto.UserDto;
import com.ds.project.common.entities.dto.request.UserRequest;
import com.ds.project.common.interfaces.IUserService;
import com.ds.project.common.mapper.UserMapper;
import com.ds.project.common.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * User service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements IUserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRolesRepository userRolesRepository;
    private final UserMapper userMapper;
    
    @Override
    public BaseResponse<UserDto> createUser(UserRequest userRequest) {
        try {
            // Check if user already exists
            if (userRepository.existsByEmail(userRequest.getEmail())) {
                return BaseResponse.<UserDto>builder()
                    .message(Optional.of("User with email " + userRequest.getEmail() + " already exists"))
                    .build();
            }
            if (userRepository.existsByUsername(userRequest.getUsername())) {
                return BaseResponse.<UserDto>builder()
                    .message(Optional.of("User with username " + userRequest.getUsername() + " already exists"))
                    .build();
            }
            
            User user = userMapper.mapToEntity(userRequest);
            user.setPassword(PasswordUtils.encodePassword(userRequest.getPassword()));
            
            // Assign roles if provided
            if (userRequest.getRoles() != null && !userRequest.getRoles().isEmpty()) {
                for (String roleName : userRequest.getRoles()) {
                    Optional<Role> roleOpt = roleRepository.findByName(roleName);
                    if (roleOpt.isPresent() && !roleOpt.get().getDeleted()) {
                        user.addRole(roleOpt.get());
                    } else {
                        log.warn("Role '{}' not found or deleted, skipping role assignment for user: {}", 
                            roleName, userRequest.getEmail());
                    }
                }
            }
            
            User savedUser = userRepository.save(user);
            
            // Force flush to ensure the user is persisted before mapping
            userRepository.flush();
            
            UserDto userDto = userMapper.mapToDto(savedUser);
            return BaseResponse.<UserDto>builder()
                .result(Optional.of(userDto))
                .build();
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            return BaseResponse.<UserDto>builder()
                .message(Optional.of("Failed to create user: " + e.getMessage()))
                .build();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<BaseResponse<UserDto>> getUserById(String id) {
        try {
            return userRepository.findById(id)
                .filter(user -> !user.getDeleted())
                .map(user -> {
                    UserDto userDto = userMapper.mapToDto(user);
                    return BaseResponse.<UserDto>builder()
                        .result(Optional.of(userDto))
                        .build();
                });
        } catch (Exception e) {
            log.error("Error getting user by id {}: {}", id, e.getMessage(), e);
            return Optional.of(BaseResponse.<UserDto>builder()
                .message(Optional.of("Failed to get user: " + e.getMessage()))
                .build());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<BaseResponse<UserDto>> getUserByEmail(String email) {
        try {
            return userRepository.findByEmail(email)
                .filter(user -> !user.getDeleted())
                .map(user -> {
                    UserDto userDto = userMapper.mapToDto(user);
                    return BaseResponse.<UserDto>builder()
                        .result(Optional.of(userDto))
                        .build();
                });
        } catch (Exception e) {
            log.error("Error getting user by email {}: {}", email, e.getMessage(), e);
            return Optional.of(BaseResponse.<UserDto>builder()
                .message(Optional.of("Failed to get user: " + e.getMessage()))
                .build());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<BaseResponse<UserDto>> getUserByUsername(String username) {
        try {
            return userRepository.findByUsername(username)
                .filter(user -> !user.getDeleted())
                .map(user -> {
                    UserDto userDto = userMapper.mapToDto(user);
                    return BaseResponse.<UserDto>builder()
                        .result(Optional.of(userDto))
                        .build();
                });
        } catch (Exception e) {
            log.error("Error getting user by username {}: {}", username, e.getMessage(), e);
            return Optional.of(BaseResponse.<UserDto>builder()
                .message(Optional.of("Failed to get user: " + e.getMessage()))
                .build());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public BaseResponse<PagedData<Page, UserDto>> getAllUsers() {
        try {
            List<User> users = userRepository.findAll().stream()
                .filter(user -> !user.getDeleted())
                .collect(Collectors.toList());
            
            List<UserDto> userDtos = users.stream()
                .map(userMapper::mapToDto)
                .collect(Collectors.toList());
            
            Page page = Page.builder()
                .page(0)
                .size(userDtos.size())
                .totalElements((long) userDtos.size())
                .totalPages(1)
                .build();
            
            PagedData<Page, UserDto> pagedData = PagedData.<Page, UserDto>builder()
                .data(userDtos)
                .page(page)
                .build();
            
            return BaseResponse.<PagedData<Page, UserDto>>builder()
                .result(Optional.of(pagedData))
                .build();
        } catch (Exception e) {
            log.error("Error getting all users: {}", e.getMessage(), e);
            return BaseResponse.<PagedData<Page, UserDto>>builder()
                .message(Optional.of("Failed to get users: " + e.getMessage()))
                .build();
        }
    }
    
    @Override
    @Transactional
    public BaseResponse<UserDto> updateUser(String id, UserRequest userRequest) {
        try {
            User existingUser = userRepository.findById(id)
                .filter(user -> !user.getDeleted())
                .orElse(null);
            
            if (existingUser == null) {
                return BaseResponse.<UserDto>builder()
                    .message(Optional.of("User not found with id: " + id))
                    .build();
            }
            
            // Check if new email conflicts with existing user
            if (!existingUser.getEmail().equals(userRequest.getEmail()) && 
                userRepository.existsByEmail(userRequest.getEmail())) {
                return BaseResponse.<UserDto>builder()
                    .message(Optional.of("User with email " + userRequest.getEmail() + " already exists"))
                    .build();
            }
            
            // Check if new username conflicts with existing user
            if (!existingUser.getUsername().equals(userRequest.getUsername()) && 
                userRepository.existsByUsername(userRequest.getUsername())) {
                return BaseResponse.<UserDto>builder()
                    .message(Optional.of("User with username " + userRequest.getUsername() + " already exists"))
                    .build();
            }
            
            existingUser.setEmail(userRequest.getEmail());
            existingUser.setUsername(userRequest.getUsername());
            existingUser.setFirstName(userRequest.getFirstName());
            existingUser.setLastName(userRequest.getLastName());
            existingUser.setPassword(PasswordUtils.encodePassword(userRequest.getPassword()));
            existingUser.setUpdatedAt(LocalDateTime.now());
            
            // Update roles if provided
            if (userRequest.getRoles() != null) {
                // Clear existing roles
                if (existingUser.getUserRoles() != null) {
                    // Remove all existing user roles from database
                    userRolesRepository.deleteByUserId(existingUser.getId());
                    existingUser.getUserRoles().clear();
                }
                
                // Add new roles
                for (String roleName : userRequest.getRoles()) {
                    Optional<Role> roleOpt = roleRepository.findByName(roleName);
                    if (roleOpt.isPresent() && !roleOpt.get().getDeleted()) {
                        existingUser.addRole(roleOpt.get());
                    } else {
                        log.warn("Role '{}' not found or deleted, skipping role assignment for user: {}", 
                            roleName, existingUser.getEmail());
                    }
                }
            }
            
            User updatedUser = userRepository.save(existingUser);
            
            UserDto userDto = userMapper.mapToDto(updatedUser);
            return BaseResponse.<UserDto>builder()
                .result(Optional.of(userDto))
                .build();
        } catch (Exception e) {
            log.error("Error updating user {}: {}", id, e.getMessage(), e);
            return BaseResponse.<UserDto>builder()
                .message(Optional.of("Failed to update user: " + e.getMessage()))
                .build();
        }
    }
    
    @Override
    @Transactional
    public void deleteUser(String id) {
        try {
            User user = userRepository.findById(id)
                .filter(u -> !u.getDeleted())
                .orElse(null);
            
            if (user == null) {
                log.warn("User not found with id: {}", id);
                return;
            }
            
            user.setDeleted(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            log.info("Successfully soft deleted user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error deleting user {}: {}", id, e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public BaseResponse<UserDto> assignRole(String userId, String roleId) {
        try {
            log.info("Assigning role {} to user {}", roleId, userId);
            
            User user = userRepository.findById(userId)
                .filter(u -> !u.getDeleted())
                .orElse(null);
            
            if (user == null) {
                return BaseResponse.<UserDto>builder()
                    .message(Optional.of("User not found with id: " + userId))
                    .build();
            }
            
            Role role = roleRepository.findById(roleId)
                .filter(r -> !r.getDeleted())
                .orElse(null);
            
            if (role == null) {
                return BaseResponse.<UserDto>builder()
                    .message(Optional.of("Role not found with id: " + roleId))
                    .build();
            }
            
            // Check if user already has this role
            if (userRolesRepository.findByUserAndRole(user, role).isPresent()) {
                return BaseResponse.<UserDto>builder()
                    .message(Optional.of("User already has role: " + role.getName()))
                    .build();
            }
            
            UserRoles userRole = UserRoles.builder()
                .user(user)
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();
            
            userRolesRepository.save(userRole);
            user.addRole(role);
            
            UserDto userDto = userMapper.mapToDto(user);
            return BaseResponse.<UserDto>builder()
                .result(Optional.of(userDto))
                .build();
        } catch (Exception e) {
            log.error("Error assigning role {} to user {}: {}", roleId, userId, e.getMessage(), e);
            return BaseResponse.<UserDto>builder()
                .message(Optional.of("Failed to assign role: " + e.getMessage()))
                .build();
        }
    }
    
    @Override
    @Transactional
    public BaseResponse<UserDto> removeRole(String userId, String roleId) {
        try {
            User user = userRepository.findById(userId)
                .filter(u -> !u.getDeleted())
                .orElse(null);
            
            if (user == null) {
                return BaseResponse.<UserDto>builder()
                    .message(Optional.of("User not found with id: " + userId))
                    .build();
            }
            
            Role role = roleRepository.findById(roleId)
                .filter(r -> !r.getDeleted())
                .orElse(null);
            
            if (role == null) {
                return BaseResponse.<UserDto>builder()
                    .message(Optional.of("Role not found with id: " + roleId))
                    .build();
            }
            
            UserRoles userRole = userRolesRepository.findByUserAndRole(user, role)
                .orElse(null);
            
            if (userRole == null) {
                return BaseResponse.<UserDto>builder()
                    .message(Optional.of("User does not have role: " + role.getName()))
                    .build();
            }
            
            userRolesRepository.delete(userRole);
            user.removeRole(role);
            
            UserDto userDto = userMapper.mapToDto(user);
            return BaseResponse.<UserDto>builder()
                .result(Optional.of(userDto))
                .build();
        } catch (Exception e) {
            log.error("Error removing role {} from user {}: {}", roleId, userId, e.getMessage(), e);
            return BaseResponse.<UserDto>builder()
                .message(Optional.of("Failed to remove role: " + e.getMessage()))
                .build();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(String userId, String roleName) {
        try {
            User user = userRepository.findById(userId)
                .filter(u -> !u.getDeleted())
                .orElse(null);
            
            if (user == null) {
                log.warn("User not found with id: {}", userId);
                return false;
            }
            
            return user.hasRole(roleName);
        } catch (Exception e) {
            log.error("Error checking role {} for user {}: {}", roleName, userId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<BaseResponse<UserDto>> authenticate(String username, String password) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username)
                .filter(user -> !user.getDeleted());
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (PasswordUtils.matches(password, user.getPassword())) {
                    log.info("User authenticated successfully: {}", username);
                    UserDto userDto = userMapper.mapToDto(user);
                    return Optional.of(BaseResponse.<UserDto>builder()
                        .result(Optional.of(userDto))
                        .build());
                } else {
                    log.warn("Authentication failed for user: {} - invalid password", username);
                    return Optional.of(BaseResponse.<UserDto>builder()
                        .message(Optional.of("Invalid password"))
                        .build());
                }
            } else {
                log.warn("Authentication failed for user: {} - user not found", username);
                return Optional.of(BaseResponse.<UserDto>builder()
                    .message(Optional.of("User not found"))
                    .build());
            }
        } catch (Exception e) {
            log.error("Error authenticating user {}: {}", username, e.getMessage(), e);
            return Optional.of(BaseResponse.<UserDto>builder()
                .message(Optional.of("Authentication failed: " + e.getMessage()))
                .build());
        }
    }
    
    /**
     * Authenticate user by email or username
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<BaseResponse<UserDto>> authenticateByEmailOrUsername(String emailOrUsername, String password) {
        try {
            Optional<User> userOpt = Optional.empty();
            
            // Try to find user by email first
            if (emailOrUsername.contains("@")) {
                userOpt = userRepository.findByEmail(emailOrUsername)
                    .filter(user -> !user.getDeleted());
            }
            
            // If not found by email, try username
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsername(emailOrUsername)
                    .filter(user -> !user.getDeleted());
            }
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (PasswordUtils.matches(password, user.getPassword())) {
                    log.info("User authenticated successfully: {}", emailOrUsername);
                    UserDto userDto = userMapper.mapToDto(user);
                    return Optional.of(BaseResponse.<UserDto>builder()
                        .result(Optional.of(userDto))
                        .build());
                } else {
                    log.warn("Authentication failed for user: {} - invalid password", emailOrUsername);
                    return Optional.of(BaseResponse.<UserDto>builder()
                        .message(Optional.of("Invalid password"))
                        .build());
                }
            } else {
                log.warn("Authentication failed for user: {} - user not found", emailOrUsername);
                return Optional.of(BaseResponse.<UserDto>builder()
                    .message(Optional.of("User not found"))
                    .build());
            }
        } catch (Exception e) {
            log.error("Error authenticating user {}: {}", emailOrUsername, e.getMessage(), e);
            return Optional.of(BaseResponse.<UserDto>builder()
                .message(Optional.of("Authentication failed: " + e.getMessage()))
                .build());
        }
    }
    
}
