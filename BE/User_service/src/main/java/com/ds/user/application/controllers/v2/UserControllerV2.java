package com.ds.user.application.controllers.v2;

import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.common.BaseResponse;
import com.ds.user.common.entities.common.PagingRequestV2;
import com.ds.user.common.entities.common.paging.PagedData;
import com.ds.user.common.entities.dto.user.UserDto;
import com.ds.user.common.interfaces.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    @Operation(summary = "Get users with enhanced filtering and sorting (V2 - Operations between each pair)")
    public ResponseEntity<BaseResponse<PagedData<UserDto>>> getUsers(@Valid @RequestBody PagingRequestV2 query) {
        log.info("POST /api/v2/users - Get users with enhanced filtering (V2)");
        log.debug("Query payload: {}", query);
        
        try {
            // Get users using V2 service (enhanced filtering)
            PagedData<User> userPage = userService.getUsersV2(query);
            
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
            log.error("Error getting users (V2): {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to get users: " + e.getMessage()));
        }
    }
}
