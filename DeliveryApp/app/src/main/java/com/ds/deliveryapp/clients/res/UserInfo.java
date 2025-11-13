package com.ds.deliveryapp.clients.res;
import lombok.Data;

@Data
public class UserInfo {
    private UserBasicInfo result;

    @Data
    public static class UserBasicInfo {
        private String id;
        private String keycloakId;
        private String firstName;
        private String lastName;
        private String email;
        private String username;
        private String phone;
    }
}
