package knp.ptithcm.datn.user_module.modules.user.lab;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import knp.ptithcm.datn.user_module.modules.base.grpc.*;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import static org.junit.jupiter.api.Assertions.*;

@Tag("lab")
class GrpcLabTest {
    private static ManagedChannel channel;
    private static String createdUserId;
    private static String createdUsername;
    private static String createdEmail;

    @BeforeAll
    static void connectRealServer() {
        String host = System.getenv().getOrDefault("USER_MODULE_GRPC_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("USER_MODULE_GRPC_PORT", "21001"));
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        try {
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
            stub.phoneExists(PhoneExistsRequestGrpc.newBuilder().setPhone("+84000000000").build());
            String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            createdUsername = "lab_" + unique;
            createdEmail = "lab_" + unique + "@example.com";
            String phone = "+84" + (System.currentTimeMillis() % 1000000000L);
            UserResponseGrpc createResp = stub.createUser(CreateUserRequestGrpc.newBuilder()
                    .setUsername(createdUsername)
                    .setEmail(createdEmail)
                    .setFirstName("Lab")
                    .setLastName("User")
                    .setPassword("Pw1!abc")
                    .setPhone(phone)
                    .setAddress("Lab Address")
                    .setIdentityNumber("ID-" + unique)
                    .build());
            createdUserId = createResp.getUser().getId();
        } catch (Exception ex) {
            if (channel != null) {
                channel.shutdownNow();
            }
            Assumptions.assumeTrue(false, "Real gRPC server not available at " + host + ":" + port + ". Skipping lab tests.");
        }
    }

    @AfterAll
    static void shutdown() {
        try {
            if (createdUserId != null) {
                UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
                stub.deleteUser(DeleteUserRequestGrpc.newBuilder().setId(createdUserId).build());
            }
        } catch (Exception ignored) {}
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @Test
    void getUserById_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        UserResponseGrpc resp = stub.getUserById(GetUserByIdRequestGrpc.newBuilder().setId(createdUserId).build());
        assertNotNull(resp);
    }

    @Test
    void getUserByUsername_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        UserResponseGrpc resp = stub.getUserByUsername(GetUserByUsernameRequestGrpc.newBuilder().setUsername(createdUsername).build());
        assertNotNull(resp);
    }

    @Test
    void getUserByEmail_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        UserResponseGrpc resp = stub.getUserByEmail(GetUserByEmailRequestGrpc.newBuilder().setEmail(createdEmail).build());
        assertNotNull(resp);
    }

    @Test
    void listUsers_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        ListUsersResponseGrpc resp = stub.listUsers(ListUsersRequestGrpc.newBuilder().setPage(0).setSize(1).build());
        assertNotNull(resp);
    }

    @Test
    void updateUserStatus_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        UpdateUserStatusResponseGrpc resp = stub.updateUserStatus(UpdateUserStatusRequestGrpc.newBuilder().setId(createdUserId).setStatus(1).build());
        assertNotNull(resp);
    }

    @Test
    void updateUserRole_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        UpdateUserRoleResponseGrpc resp = stub.updateUserRole(UpdateUserRoleRequestGrpc.newBuilder().setId(createdUserId).setRole(1).build());
        assertNotNull(resp);
    }

    @Test
    void createUser_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        UserResponseGrpc resp = stub.createUser(CreateUserRequestGrpc.newBuilder()
                .setUsername("labc_" + unique)
                .setEmail("labc_" + unique + "@example.com")
                .setPassword("Pw1!abc")
                .build());
        try {
            stub.deleteUser(DeleteUserRequestGrpc.newBuilder().setId(resp.getUser().getId()).build());
        } catch (Exception ignored) {}
        assertNotNull(resp);
    }

    @Test
    void updateUser_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        UserResponseGrpc resp = stub.updateUser(UpdateUserRequestGrpc.newBuilder()
                .setId(createdUserId)
                .setFirstName("New")
                .setLastName("Name")
                .build());
        assertNotNull(resp);
    }

    @Test
    void updateUserPassword_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        UserResponseGrpc resp = stub.updateUserPassword(UpdateUserPasswordRequestGrpc.newBuilder().setId(createdUserId).setNewPassword("P@ss123!").build());
        assertNotNull(resp);
    }

    @Test
    void deleteUser_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        UserResponseGrpc temp = stub.createUser(CreateUserRequestGrpc.newBuilder()
                .setUsername("labd_" + unique)
                .setEmail("labd_" + unique + "@example.com")
                .setPassword("Pw1!abc")
                .build());
        DeleteUserResponseGrpc resp = stub.deleteUser(DeleteUserRequestGrpc.newBuilder().setId(temp.getUser().getId()).build());
        assertNotNull(resp);
    }

    @Test
    void registerByPhone_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        String phone = "+84" + (System.currentTimeMillis() % 1000000000L);
        UserResponseGrpc resp = stub.registerByPhone(RegisterByPhoneRequestGrpc.newBuilder().setPhone(phone).setPassword("Pw1!abc").setFirstName("A").setLastName("B").build());
        try {
            stub.deleteUser(DeleteUserRequestGrpc.newBuilder().setId(resp.getUser().getId()).build());
        } catch (Exception ignored) {}
        assertNotNull(resp);
    }

    @Test
    void phoneExists_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        PhoneExistsResponseGrpc resp = stub.phoneExists(PhoneExistsRequestGrpc.newBuilder().setPhone("+84111111111").build());
        assertNotNull(resp);
    }

    @Test
    void sendOtp_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        SendOtpResponseGrpc resp = stub.sendOtp(SendOtpRequestGrpc.newBuilder().setPhone("+8401").build());
        assertNotNull(resp);
    }

    @Test
    void verifyOtp_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        VerifyOtpResponseGrpc resp = stub.verifyOtp(VerifyOtpRequestGrpc.newBuilder().setPhone("+8401").setOtp("000000").build());
        assertNotNull(resp);
    }

    @Test
    void resetPasswordWithOtp_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        ResetPasswordWithOtpResponseGrpc resp = stub.resetPasswordWithOtp(ResetPasswordWithOtpRequestGrpc.newBuilder().setPhone("+8401").setOtp("000000").setNewPassword("Pw1!").build());
        assertNotNull(resp);
    }

    @Test
    void updateProfile_success_call() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        UserResponseGrpc resp = stub.updateProfile(UpdateProfileRequestGrpc.newBuilder()
                .setId(createdUserId)
                .setFirstName("New")
                .setLastName("Name")
                .setPhone("+84" + (System.currentTimeMillis() % 1000000000L))
                .setAddress("A")
                .setIdentityNumber("ID7")
                .build());
        assertNotNull(resp);
    }
}
