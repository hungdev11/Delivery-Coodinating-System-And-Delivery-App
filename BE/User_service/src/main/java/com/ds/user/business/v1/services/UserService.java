package com.ds.user.business.v1.services;

import com.ds.user.common.entities.base.User;
import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.common.interfaces.IUserService;

import org.springframework.beans.factory.annotation.Autowired;
import com.ds.user.common.entities.dto.common.PagedData;
import com.ds.user.common.entities.dto.common.PagingRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User updateUser(UUID id, User user) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setAddress(user.getAddress());
        return userRepository.save(existingUser);
    }

    @Override
    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> getUser(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Override
    public PagedData<User> listUsers(PagingRequest pagingRequest) {
        // Get total count
        long totalElements = userRepository.count();
        
        // Calculate pagination
        int totalPages = (int) Math.ceil((double) totalElements / pagingRequest.getSize());
        
        // Get the requested page of data
        List<User> allUsers = userRepository.findAll();
        int startIndex = pagingRequest.getPage() * pagingRequest.getSize();
        int endIndex = Math.min(startIndex + pagingRequest.getSize(), allUsers.size());
        
        List<User> pageData = allUsers.subList(startIndex, endIndex);
        
        // Create Paging object
        var paging = com.ds.user.common.entities.dto.common.Paging.<String>builder()
                .page(pagingRequest.getPage())
                .size(pagingRequest.getSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .filters(pagingRequest.getFilters() != null ? pagingRequest.getFilters() : List.of())
                .sorts(pagingRequest.getSorts() != null ? pagingRequest.getSorts() : List.of())
                .selected(pagingRequest.getSelected())
                .build();
        
        // Create PagedData
        return PagedData.<User>builder()
                .data(pageData)
                .page(paging)
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
    public User upsertByKeycloakId(String keycloakId, String username, String email, String firstName, String lastName) {
        Optional<User> existingOpt = userRepository.findByKeycloakId(keycloakId);
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            existing.setUsername(username != null ? username : existing.getUsername());
            existing.setEmail(email != null ? email : existing.getEmail());
            existing.setFirstName(firstName != null ? firstName : existing.getFirstName());
            existing.setLastName(lastName != null ? lastName : existing.getLastName());
            return userRepository.save(existing);
        }

        User user = User.builder()
                .keycloakId(keycloakId)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .status(User.UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }
}
