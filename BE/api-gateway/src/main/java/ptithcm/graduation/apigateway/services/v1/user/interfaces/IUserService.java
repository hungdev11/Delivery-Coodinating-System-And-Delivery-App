package ptithcm.graduation.apigateway.services.v1.user.interfaces;

import ptithcm.graduation.apigateway.services.v1.user.dto.*;
import ptithcm.graduation.apigateway.models.BaseResponse;
import ptithcm.graduation.apigateway.models.PagedResult;
import ptithcm.graduation.apigateway.models.Paging;

import java.util.concurrent.CompletableFuture;

public interface IUserService {
    // User CRUD operations
    CompletableFuture<BaseResponse<UserDto>> getUserById(String id);
    CompletableFuture<BaseResponse<UserDto>> getUserByUsername(String username);
    CompletableFuture<BaseResponse<UserDto>> getUserByEmail(String email);
    CompletableFuture<BaseResponse<PagedResult<UserDto>>> listUsers(Paging request);
    CompletableFuture<BaseResponse<UserDto>> createUser(CreateUserRequestDto request);
    CompletableFuture<BaseResponse<UserDto>> updateUser(UpdateUserRequestDto request);
    CompletableFuture<BaseResponse<UserDto>> getCurrentUser(String userId);
    CompletableFuture<BaseResponse<UserDto>> updateProfile(UpdateUserRequestDto request);
    CompletableFuture<BaseResponse<String>> changePassword(ChangePasswordRequestDto request);

    // User status and role management
    CompletableFuture<BaseResponse<UserDto>> updateUserStatus(String id, Integer status);
    CompletableFuture<BaseResponse<UserDto>> updateUserRole(String id, Integer role);
    
    // Profile management
    CompletableFuture<BaseResponse<UserDto>> updateProfile(String id, String firstName, String lastName, 
                                                     String phone, String address, String identityNumber);

}
