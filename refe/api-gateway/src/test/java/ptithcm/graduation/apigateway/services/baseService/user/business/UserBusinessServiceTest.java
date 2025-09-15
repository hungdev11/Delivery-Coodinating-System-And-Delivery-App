package ptithcm.graduation.apigateway.services.baseService.user.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ptithcm.graduation.apigateway.services.baseService.user.dto.*;
import ptithcm.graduation.apigateway.services.baseService.user.mapper.BaseUserMapper;
import knp.ptithcm.datn.user_module.modules.base.grpc.*;
import org.springframework.test.util.ReflectionTestUtils;
import com.google.common.util.concurrent.Futures;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserBusinessServiceTest {

    @Mock
    private UserServiceGrpc.UserServiceFutureStub userServiceStub;

    @Mock
    private BaseUserMapper userMapper;

    @InjectMocks
    private BaseUserBusinessService userBusinessService;

    private UserResponseGrpc mockUserResponseGrpc;
    private BaseUserResponseDto mockUserResponseDto;
    private ListUsersResponseGrpc mockListUsersResponseGrpc;
    private BaseListUsersResponseDto mockListUsersResponseDto;
    private BaseCreateUserRequestDto mockCreateRequest;
    private BaseUpdateUserRequestDto mockUpdateRequest;

    @BeforeEach
    void setUp() {
        // Setup mocks
        ReflectionTestUtils.setField(userBusinessService, "userServiceStub", userServiceStub);
        
        // Mock data
        String userId = UUID.randomUUID().toString();
        
        mockUserResponseGrpc = UserResponseGrpc.newBuilder()
                .setMessage("Success")
                .setUser(UserGrpc.newBuilder()
                        .setId(userId)
                        .setUsername("testuser")
                        .setEmail("test@example.com")
                        .setFirstName("Test")
                        .setLastName("User")
                        .setPhone("1234567890")
                        .setAddress("Test Address")
                        .setStatus(1)
                        .build())
                .build();

        BaseUserDto mockUserDto = new BaseUserDto();
        mockUserDto.setId(userId);
        mockUserDto.setUsername("testuser");
        mockUserDto.setEmail("test@example.com");
        mockUserDto.setFirstName("Test");
        mockUserDto.setLastName("User");
        mockUserDto.setPhone("1234567890");
        mockUserDto.setAddress("Test Address");
        mockUserDto.setStatus(1);
        
        mockUserResponseDto = new BaseUserResponseDto();
        mockUserResponseDto.setMessage("Success");
        mockUserResponseDto.setUser(mockUserDto);

        mockListUsersResponseGrpc = ListUsersResponseGrpc.newBuilder()
                .addUsers(mockUserResponseGrpc.getUser())
                .build();

        mockListUsersResponseDto = new BaseListUsersResponseDto();
        mockListUsersResponseDto.setUsers(List.of(mockUserDto));
        mockListUsersResponseDto.setTotal(1);

        mockCreateRequest = new BaseCreateUserRequestDto();
        mockCreateRequest.setUsername("newuser");
        mockCreateRequest.setEmail("new@example.com");
        mockCreateRequest.setPassword("password123");
        mockCreateRequest.setFirstName("New");
        mockCreateRequest.setLastName("User");

        mockUpdateRequest = new BaseUpdateUserRequestDto();
        mockUpdateRequest.setId(userId);
        mockUpdateRequest.setFirstName("Updated");
        mockUpdateRequest.setLastName("User");
        mockUpdateRequest.setPhone("0987654321");
        mockUpdateRequest.setAddress("Updated Address");
    }

    @Test
    void getUserById_Success() throws Exception {
        // Arrange
        String userId = UUID.randomUUID().toString();
        GetUserByIdRequestGrpc mockRequest = GetUserByIdRequestGrpc.newBuilder()
                .setId(userId)
                .build();
        
        when(userMapper.toGetByIdProto(userId)).thenReturn(mockRequest);
        when(userServiceStub.getUserById(any(GetUserByIdRequestGrpc.class)))
                .thenReturn(Futures.immediateFuture(mockUserResponseGrpc));
        when(userMapper.toDto(mockUserResponseGrpc)).thenReturn(mockUserResponseDto);

        // Act
        CompletableFuture<BaseUserResponseDto> result = userBusinessService.getUserById(userId);

        // Assert
        assertNotNull(result);
        BaseUserResponseDto actualResult = result.get();
        assertEquals(mockUserResponseDto.getUser().getId(), actualResult.getUser().getId());
        assertEquals(mockUserResponseDto.getUser().getUsername(), actualResult.getUser().getUsername());
        assertEquals(mockUserResponseDto.getUser().getEmail(), actualResult.getUser().getEmail());
        
        verify(userMapper).toGetByIdProto(userId);
        verify(userServiceStub).getUserById(mockRequest);
        verify(userMapper).toDto(mockUserResponseGrpc);
    }

    @Test
    void getUserById_ThrowsException() throws Exception {
        // Arrange
        String userId = UUID.randomUUID().toString();
        GetUserByIdRequestGrpc mockRequest = GetUserByIdRequestGrpc.newBuilder()
                .setId(userId)
                .build();
        
        when(userMapper.toGetByIdProto(userId)).thenReturn(mockRequest);
        when(userServiceStub.getUserById(any(GetUserByIdRequestGrpc.class)))
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("gRPC Error")));

        // Act & Assert
        CompletableFuture<BaseUserResponseDto> result = userBusinessService.getUserById(userId);
        
        ExecutionException executionException = assertThrows(ExecutionException.class, () -> result.get());
        assertTrue(executionException.getCause() instanceof RuntimeException);
        assertEquals("Error getting user by id: java.lang.RuntimeException: gRPC Error", executionException.getCause().getMessage());
        
        verify(userMapper).toGetByIdProto(userId);
        verify(userServiceStub).getUserById(mockRequest);
    }

    @Test
    void getUserByUsername_Success() throws Exception {
        // Arrange
        String username = "testuser";
        GetUserByUsernameRequestGrpc mockRequest = GetUserByUsernameRequestGrpc.newBuilder()
                .setUsername(username)
                .build();
        
        when(userMapper.toGetByUsernameProto(username)).thenReturn(mockRequest);
        when(userServiceStub.getUserByUsername(any(GetUserByUsernameRequestGrpc.class)))
                .thenReturn(Futures.immediateFuture(mockUserResponseGrpc));
        when(userMapper.toDto(mockUserResponseGrpc)).thenReturn(mockUserResponseDto);

        // Act
        CompletableFuture<BaseUserResponseDto> result = userBusinessService.getUserByUsername(username);

        // Assert
        assertNotNull(result);
        BaseUserResponseDto actualResult = result.get();
        assertEquals(mockUserResponseDto.getUser().getUsername(), actualResult.getUser().getUsername());
        
        verify(userMapper).toGetByUsernameProto(username);
        verify(userServiceStub).getUserByUsername(mockRequest);
        verify(userMapper).toDto(mockUserResponseGrpc);
    }

    @Test
    void getUserByEmail_Success() throws Exception {
        // Arrange
        String email = "test@example.com";
        GetUserByEmailRequestGrpc mockRequest = GetUserByEmailRequestGrpc.newBuilder()
                .setEmail(email)
                .build();
        
        when(userMapper.toGetByEmailProto(email)).thenReturn(mockRequest);
        when(userServiceStub.getUserByEmail(any(GetUserByEmailRequestGrpc.class)))
                .thenReturn(Futures.immediateFuture(mockUserResponseGrpc));
        when(userMapper.toDto(mockUserResponseGrpc)).thenReturn(mockUserResponseDto);

        // Act
        CompletableFuture<BaseUserResponseDto> result = userBusinessService.getUserByEmail(email);

        // Assert
        assertNotNull(result);
        BaseUserResponseDto actualResult = result.get();
        assertEquals(mockUserResponseDto.getUser().getEmail(), actualResult.getUser().getEmail());
        
        verify(userMapper).toGetByEmailProto(email);
        verify(userServiceStub).getUserByEmail(mockRequest);
        verify(userMapper).toDto(mockUserResponseGrpc);
    }

    @Test
    void listUsers_Success() throws Exception {
        // Arrange
        BaseListUsersRequestDto mockRequestDto = new BaseListUsersRequestDto();
        mockRequestDto.setPage(1);
        mockRequestDto.setSize(10);
        
        ListUsersRequestGrpc mockRequestGrpc = ListUsersRequestGrpc.newBuilder()
                .setPage(1)
                .setSize(10)
                .build();
        
        when(userMapper.toProto(mockRequestDto)).thenReturn(mockRequestGrpc);
        when(userServiceStub.listUsers(any(ListUsersRequestGrpc.class)))
                .thenReturn(Futures.immediateFuture(mockListUsersResponseGrpc));
        when(userMapper.toDto(mockListUsersResponseGrpc)).thenReturn(mockListUsersResponseDto);

        // Act
        CompletableFuture<BaseListUsersResponseDto> result = userBusinessService.listUsers(mockRequestDto);

        // Assert
        assertNotNull(result);
        BaseListUsersResponseDto actualResult = result.get();
        assertEquals(mockListUsersResponseDto.getTotal(), actualResult.getTotal());
        assertEquals(mockListUsersResponseDto.getUsers().size(), actualResult.getUsers().size());
        
        verify(userMapper).toProto(mockRequestDto);
        verify(userServiceStub).listUsers(mockRequestGrpc);
        verify(userMapper).toDto(mockListUsersResponseGrpc);
    }

    @Test
    void createUser_Success() throws Exception {
        // Arrange
        CreateUserRequestGrpc mockRequestGrpc = CreateUserRequestGrpc.newBuilder()
                .setUsername("newuser")
                .setEmail("new@example.com")
                .setPassword("password123")
                .setFirstName("New")
                .setLastName("User")
                .build();
        
        when(userMapper.toProto(mockCreateRequest)).thenReturn(mockRequestGrpc);
        when(userServiceStub.createUser(any(CreateUserRequestGrpc.class)))
                .thenReturn(Futures.immediateFuture(mockUserResponseGrpc));
        when(userMapper.toDto(mockUserResponseGrpc)).thenReturn(mockUserResponseDto);

        // Act
        CompletableFuture<BaseUserResponseDto> result = userBusinessService.createUser(mockCreateRequest);

        // Assert
        assertNotNull(result);
        BaseUserResponseDto actualResult = result.get();
        assertEquals(mockUserResponseDto.getUser().getId(), actualResult.getUser().getId());
        
        verify(userMapper).toProto(mockCreateRequest);
        verify(userServiceStub).createUser(mockRequestGrpc);
        verify(userMapper).toDto(mockUserResponseGrpc);
    }

    @Test
    void updateUser_Success() throws Exception {
        // Arrange
        UpdateUserRequestGrpc mockRequestGrpc = UpdateUserRequestGrpc.newBuilder()
                .setId(mockUpdateRequest.getId())
                .setFirstName("Updated")
                .setLastName("User")
                .setPhone("0987654321")
                .setAddress("Updated Address")
                .build();
        
        when(userMapper.toProto(mockUpdateRequest)).thenReturn(mockRequestGrpc);
        when(userServiceStub.updateUser(any(UpdateUserRequestGrpc.class)))
                .thenReturn(Futures.immediateFuture(mockUserResponseGrpc));
        when(userMapper.toDto(mockUserResponseGrpc)).thenReturn(mockUserResponseDto);

        // Act
        CompletableFuture<BaseUserResponseDto> result = userBusinessService.updateUser(mockUpdateRequest);

        // Assert
        assertNotNull(result);
        BaseUserResponseDto actualResult = result.get();
        assertEquals(mockUserResponseDto.getUser().getId(), actualResult.getUser().getId());
        
        verify(userMapper).toProto(mockUpdateRequest);
        verify(userServiceStub).updateUser(mockRequestGrpc);
        verify(userMapper).toDto(mockUserResponseGrpc);
    }

    @Test
    void updateUserPassword_Success() throws Exception {
        // Arrange
        String userId = UUID.randomUUID().toString();
        String newPassword = "newpassword123";
        UpdateUserPasswordRequestGrpc mockRequest = UpdateUserPasswordRequestGrpc.newBuilder()
                .setId(userId)
                .setNewPassword(newPassword)
                .build();
        
        when(userMapper.toUpdatePasswordProto(userId, newPassword)).thenReturn(mockRequest);
        when(userServiceStub.updateUserPassword(any(UpdateUserPasswordRequestGrpc.class)))
                .thenReturn(Futures.immediateFuture(mockUserResponseGrpc));
        when(userMapper.toDto(mockUserResponseGrpc)).thenReturn(mockUserResponseDto);

        // Act
        CompletableFuture<BaseUserResponseDto> result = userBusinessService.updateUserPassword(userId, newPassword);

        // Assert
        assertNotNull(result);
        BaseUserResponseDto actualResult = result.get();
        assertEquals(mockUserResponseDto.getUser().getId(), actualResult.getUser().getId());
        
        verify(userMapper).toUpdatePasswordProto(userId, newPassword);
        verify(userServiceStub).updateUserPassword(mockRequest);
        verify(userMapper).toDto(mockUserResponseGrpc);
    }

    @Test
    void deleteUser_Success() throws Exception {
        // Arrange
        String userId = UUID.randomUUID().toString();
        DeleteUserRequestGrpc mockRequest = DeleteUserRequestGrpc.newBuilder()
                .setId(userId)
                .build();
        
        when(userMapper.toDeleteProto(userId)).thenReturn(mockRequest);
        when(userServiceStub.deleteUser(any(DeleteUserRequestGrpc.class)))
                .thenReturn(Futures.immediateFuture(DeleteUserResponseGrpc.newBuilder().setMessage("Deleted").build()));

        // Act
        CompletableFuture<String> result = userBusinessService.deleteUser(userId);

        // Assert
        assertNotNull(result);
        assertDoesNotThrow(() -> result.get());
        
        verify(userMapper).toDeleteProto(userId);
        verify(userServiceStub).deleteUser(mockRequest);
    }
}
