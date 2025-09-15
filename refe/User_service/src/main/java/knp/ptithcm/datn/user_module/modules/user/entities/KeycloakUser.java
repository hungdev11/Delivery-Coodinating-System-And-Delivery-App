package knp.ptithcm.datn.user_module.modules.user.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakUser {
    private String id; // sub
    private String username;
    private String email;
    private Boolean emailVerified;
    private Boolean enabled;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private Map<String, Object> attributes;
    private String provider; // keycloak, google, ...
    private Map<String, String> oauthIds; // googleId, facebookId, ...
}
