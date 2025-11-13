package com.ds.user.application.controllers.v0;

import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.common.BaseResponse;
import com.ds.user.common.entities.common.PagingRequestV0;
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

/**
 * V0 API Controller for User Management
 * V0: Simple paging and sorting without dynamic filters
 * Filters must be defined by the caller
 */
@Slf4j
@RestController
@RequestMapping("/api/v0/users")
@RequiredArgsConstructor
@Tag(name = "Users V0", description = "User Management API V0 - Simple Paging & Sorting")
public class UserControllerV0 {

    private final IUserService userService;
    private final IExternalAuthFacade externalAuthFacade;

    @PostMapping
    @Operation(summary = "Get users with simple paging and sorting (V0 - No Dynamic Filters)")
    public ResponseEntity<BaseResponse<PagedData<UserDto>>> getUsers(@Valid @RequestBody PagingRequestV0 query) {
        log.info("POST /api/v0/users - Get users with simple paging (V0)");
        log.debug("Query payload: {}", query);
        
        try {
            // Get users using V0 service (simple paging, no filters)
            PagedData<User> userPage = userService.getUsersV0(query);
            
            // Convert to PagedData<UserDto>
            List<UserDto> userDtos = userPage.getData().stream()
                    .map(UserDto::from)
                    .toList();
            
            // Use the existing paging from userPage
            PagedData<UserDto> pagedData = PagedData.<UserDto>builder()
                    .data(userDtos)
                    .page(userPage.getPage())
                    .build();
            
            return ResponseEntity.ok(BaseResponse.success(pagedData));
            
        } catch (Exception e) {
            log.error("Error getting users (V0): {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to get users: " + e.getMessage()));
        }
    }
}
