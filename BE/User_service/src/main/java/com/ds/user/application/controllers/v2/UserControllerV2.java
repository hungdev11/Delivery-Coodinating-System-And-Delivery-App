package com.ds.user.application.controllers.v2;

import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.common.BaseResponse;
import com.ds.user.common.entities.common.PagingRequestV2;
import com.ds.user.common.entities.common.paging.PagedData;
import com.ds.user.common.entities.dto.user.UserDto;
import com.ds.user.common.interfaces.IExternalAuthFacade;
import com.ds.user.common.interfaces.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * V2 API Controller for User Management
 * V2: Enhanced dynamic filtering with operations between each pair of conditions
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
@Tag(name = "Users V2", description = "User Management API V2 - Enhanced Dynamic Filtering")
public class UserControllerV2 {

    private final IUserService userService;
    private final IExternalAuthFacade externalAuthFacade;

    @PostMapping
    @Operation(summary = "Get users with enhanced filtering and sorting (V2 - Operations between each pair)")
    public ResponseEntity<BaseResponse<PagedData<UserDto>>> getUsers(@Valid @RequestBody PagingRequestV2 query) {
        log.info("POST /api/v2/users - Get users with enhanced filtering (V2)");
        log.debug("Query payload: {}", query);
        
        try {
            // Get users using V2 service (enhanced filtering)
            PagedData<User> userPage = userService.getUsersV2(query);
            
            // Batch fetch roles for all users in parallel
            List<String> userIds = userPage.getData().stream()
                    .map(User::getId)
                    .filter(id -> id != null && !id.isBlank())
                    .toList();
            
            Map<String, List<String>> rolesMap = userIds.isEmpty() 
                    ? Collections.emptyMap() 
                    : externalAuthFacade.batchGetUserRoles(userIds);
            
            // Convert to PagedData<UserDto> with roles
            List<UserDto> userDtos = userPage.getData().stream()
                    .map(user -> buildUserDto(user, rolesMap.getOrDefault(user.getId(), Collections.emptyList())))
                    .toList();
            
            // Use the existing paging from userPage
            PagedData<UserDto> pagedData = PagedData.<UserDto>builder()
                    .data(userDtos)
                    .page(userPage.getPage())
                    .build();
            
            return ResponseEntity.ok(BaseResponse.success(pagedData));
            
        } catch (Exception e) {
            log.error("Error getting users (V2): {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to get users: " + e.getMessage()));
        }
    }

    private UserDto buildUserDto(User user) {
        return buildUserDto(user, null);
    }
    
    private UserDto buildUserDto(User user, List<String> roles) {
        if (user == null) {
            return null;
        }

        // If roles are provided (from batch fetch), use them
        // Otherwise, return empty list (roles should be provided via batch fetch)
        if (roles == null) {
            roles = Collections.emptyList();
        }

        return UserDto.from(user, roles);
    }
}
