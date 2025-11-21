package com.ds.user.business.v1.services;

import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.common.PagingRequest;
import com.ds.user.common.entities.common.PagingRequestV0;
import com.ds.user.common.entities.common.PagingRequestV2;
import com.ds.user.common.entities.common.paging.PagedData;
import com.ds.user.common.helper.FilterableFieldRegistry;
import com.ds.user.common.helper.GenericQueryService;
import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.common.interfaces.IUserService;

import org.springframework.beans.factory.annotation.Autowired;

import com.ds.user.common.utils.EnhancedQueryParser;
import com.ds.user.common.utils.EnhancedQueryParserV2;
import com.ds.user.infrastructure.kafka.UserEventPublisher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FilterableFieldRegistry fieldRegistry;

    @Autowired(required = false)
    private UserEventPublisher userEventPublisher;

    @Override
    public User createUser(User user) {
        User saved = userRepository.save(user);
        // Publish event for snapshot synchronization
        if (userEventPublisher != null) {
            userEventPublisher.publishUserCreated(saved);
        }
        return saved;
    }

    @Override
    public User updateUser(String id, User user) { // Changed from UUID to String
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setAddress(user.getAddress());
        User saved = userRepository.save(existingUser);
        // Publish event for snapshot synchronization
        if (userEventPublisher != null) {
            userEventPublisher.publishUserUpdated(saved);
        }
        return saved;
    }

    @Override
    public void deleteUser(String id) { // Changed from UUID to String
        // Publish event before deletion
        if (userEventPublisher != null) {
            userEventPublisher.publishUserDeleted(id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> getUser(String id) { // Changed from UUID to String
        return userRepository.findById(id);
    }

    @Override
    public PagedData<User> getUsers(PagingRequest query) {
        // Initialize field registry for User entity if not already done
        if (fieldRegistry.getFilterableFields(User.class).isEmpty()) {
            fieldRegistry.autoDiscoverFields(User.class);
        }

        // Set the field registry for enhanced query parser
        EnhancedQueryParser.setFieldRegistry(fieldRegistry);

        // Use GenericQueryService for consistent query handling
        return GenericQueryService.executeQuery(userRepository, query, User.class);
    }

    @Override
    public PagedData<User> getUsersV0(PagingRequestV0 query) {
        // V0: Simple paging with sorting only, no dynamic filters
        // Create pageable with sorting
        Sort sort = query.getSortsOrEmpty().isEmpty() 
            ? Sort.by(Sort.Direction.DESC, "id")
            : EnhancedQueryParser.parseSortConfigs(query.getSortsOrEmpty(), User.class);
        
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), sort);
        
        // Execute query without filters
        Page<User> page = userRepository.findAll(pageable);
        
        // Build paged data response
        return PagedData.<User>builder()
                .data(page.getContent())
                .page(new com.ds.user.common.entities.common.paging.Paging<>(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        null, // No filters in V0
                        query.getSortsOrEmpty(),
                        query.getSelectedOrEmpty()
                ))
                .build();
    }

    @Override
    public PagedData<User> getUsersV2(PagingRequestV2 query) {
        // V2: Enhanced filtering with operations between each pair
        // Initialize field registry
        if (fieldRegistry.getFilterableFields(User.class).isEmpty()) {
            fieldRegistry.autoDiscoverFields(User.class);
        }

        // Create specification from V2 filters
        Specification<User> spec = Specification.where(null);
        if (query.getFiltersOrNull() != null) {
            spec = EnhancedQueryParserV2.parseFilterGroup(query.getFiltersOrNull(), User.class);
        }

        // Create sort
        Sort sort = query.getSortsOrEmpty().isEmpty()
            ? Sort.by(Sort.Direction.DESC, "id")
            : EnhancedQueryParser.parseSortConfigs(query.getSortsOrEmpty(), User.class);

        // Create pageable
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), sort);

        // Execute query
        Page<User> page = userRepository.findAll(spec, pageable);

        // Build paged data response
        return PagedData.<User>builder()
                .data(page.getContent())
                .page(new com.ds.user.common.entities.common.paging.Paging<>(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        null, // V2 filters structure is different, don't return it in paging
                        query.getSortsOrEmpty(),
                        query.getSelectedOrEmpty()
                ))
                .build();
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User upsertByKeycloakId(String keycloakId, String username, String email, String firstName,
            String lastName) {
        // Since ID is now the Keycloak ID, we can use findById directly
        Optional<User> existingOpt = userRepository.findById(keycloakId);
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            existing.setUsername(username != null ? username : existing.getUsername());
            existing.setEmail(email != null ? email : existing.getEmail());
            existing.setFirstName(firstName != null ? firstName : existing.getFirstName());
            existing.setLastName(lastName != null ? lastName : existing.getLastName());
            User saved = userRepository.save(existing);
            // Publish event for snapshot synchronization
            if (userEventPublisher != null) {
                userEventPublisher.publishUserUpdated(saved);
            }
            return saved;
        }

        // Create new user with Keycloak ID as the primary key
        User user = User.builder()
                .id(keycloakId) // Use Keycloak ID as the primary key
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .status(User.UserStatus.ACTIVE)
                .build();
        User saved = userRepository.save(user);
        // Publish event for snapshot synchronization
        if (userEventPublisher != null) {
            userEventPublisher.publishUserCreated(saved);
        }
        return saved;
    }

}
