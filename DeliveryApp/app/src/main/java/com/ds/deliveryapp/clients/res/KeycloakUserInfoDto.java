package com.ds.deliveryapp.clients.res;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakUserInfoDto {
    private String sub; // Keycloak user ID
    private String preferredUsername;
    private String email;
    private String givenName;
    private String familyName;
    private String name;
    private Boolean emailVerified;
    private List<String> roles;
    private Map<String, Object> realmAccess;
    private Map<String, Object> resourceAccess;
    private Long exp; // Expiration time
    private Long iat; // Issued at time
    private String iss; // Issuer
    private String aud; // Audience
}
