package knp.ptithcm.datn.user_module.helper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import org.springframework.core.convert.converter.Converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class JWTAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    @Value("${jwt.converter.resource-id:}")
    private String resourceId;

    @Value("${jwt.converter.principal-attribute:sub}")
    private String principalAttribute;

    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new java.util.HashSet<>();
        authorities.addAll(defaultGrantedAuthoritiesConverter.convert(jwt));
        authorities.addAll(extractResourceRoles(jwt));
        String principal = getPrincipalClaimName(jwt);
        return new JwtAuthenticationToken(jwt, authorities, principal);
    }

    private String getPrincipalClaimName(Jwt jwt) {
        return jwt.getClaimAsString(principalAttribute);
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        Collection<String> allRoles = new ArrayList<>();
        if (resourceAccess != null && resourceAccess.get(resourceId) != null) {
            Map<String, Object> resource = (Map<String, Object>) resourceAccess.get(resourceId);
            if (resource != null && resource.get("roles") != null) {
                Collection<String> resourceRoles = (Collection<String>) resource.get("roles");
                allRoles.addAll(resourceRoles);
            }
        }
        if (realmAccess != null && realmAccess.get("roles") != null) {
            Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
            allRoles.addAll(realmRoles);
        }
        if (allRoles.isEmpty() || !resourceId.equals(jwt.getClaimAsString("azp"))) {
            return java.util.Set.of();
        }
        return allRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(java.util.stream.Collectors.toSet());
    }
}
