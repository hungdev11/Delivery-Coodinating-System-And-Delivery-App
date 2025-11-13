package com.ds.session.session_service.common.entities.dto.response;

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
        private String phone;
    } 
}
