package ptithcm.graduation.apigateway.services.v1.user.business;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ptithcm.graduation.apigateway.models.BaseResponse;
import ptithcm.graduation.apigateway.models.PagedResult;
import ptithcm.graduation.apigateway.models.Paging;
import ptithcm.graduation.apigateway.services.baseService.user.interfaces.IBaseUserBusinessService;
import ptithcm.graduation.apigateway.services.v1.user.dto.*;
import ptithcm.graduation.apigateway.services.v1.user.interfaces.IUserService;
import ptithcm.graduation.apigateway.services.v1.user.mapper.UserMapper;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {  
    
    private final UserMapper userMapper;
    private final IBaseUserBusinessService userBusinessService;
    
    @Override
    public CompletableFuture<BaseResponse<UserDto>> getCurrentUser(String userId) {
        try {
            log.info("Getting current user with ID: {}", userId);
            
            return userBusinessService.getUserById(userId)
                .thenApply(userResponse -> {
                    if (userResponse.getUser() != null) {
                        UserDto userProfile = userMapper.toDto(userResponse.getUser());
                        return new BaseResponse<>("User retrieved successfully", userProfile);
                    } else {
                        throw new RuntimeException("User not found");
                    }
                });
            
        } catch (Exception e) {
            log.error("Error getting current user with ID: {}", userId, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<BaseResponse<UserDto>> updateProfile(UpdateUserRequestDto request) {
        try {
            log.info("Processing profile update for user ID: {}", request.getId());
            
            // Call baseService updateProfile method
            return userBusinessService.updateProfile(
                request.getId(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhone(),
                request.getAddress(),
                request.getIdentityNumber()
            ).thenApply(baseResponse -> {
                if (baseResponse.getUser() != null) {
                    UserDto userProfile = userMapper.toDto(baseResponse.getUser());
                    return new BaseResponse<>(baseResponse.getMessage(), userProfile);
                } else {
                    throw new RuntimeException("Failed to update profile");
                }
            });
            
        } catch (Exception e) {
            log.error("Error updating profile for user ID: {}", request.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<BaseResponse<String>> changePassword(ChangePasswordRequestDto request) {
        try {
            log.info("Processing password change for user ID: {}", request.getUserId());
            
            // Call baseService updateUserPassword method
            return userBusinessService.updateUserPassword(request.getUserId(), request.getNewPassword())
                .thenApply(baseResponse -> new BaseResponse<>(baseResponse.getMessage(), "Password changed successfully"));
                
        } catch (Exception e) {
            log.error("Error changing password for user ID: {}", request.getUserId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<BaseResponse<UserDto>> getUserById(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserById'");
    }

    @Override
    public CompletableFuture<BaseResponse<UserDto>> getUserByUsername(String username) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserByUsername'");
    }

    @Override
    public CompletableFuture<BaseResponse<UserDto>> getUserByEmail(String email) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserByEmail'");
    }

    @Override
    public CompletableFuture<BaseResponse<PagedResult<UserDto>>> listUsers(Paging request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listUsers'");
    }

    @Override
    public CompletableFuture<BaseResponse<UserDto>> createUser(CreateUserRequestDto request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createUser'");
    }

    @Override
    public CompletableFuture<BaseResponse<UserDto>> updateUser(UpdateUserRequestDto request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateUser'");
    }

    @Override
    public CompletableFuture<BaseResponse<UserDto>> updateUserStatus(String id, Integer status) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateUserStatus'");
    }

    @Override
    public CompletableFuture<BaseResponse<UserDto>> updateUserRole(String id, Integer role) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateUserRole'");
    }

    @Override
    public CompletableFuture<BaseResponse<UserDto>> updateProfile(String id, String firstName, String lastName,
            String phone, String address, String identityNumber) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateProfile'");
    }
}
