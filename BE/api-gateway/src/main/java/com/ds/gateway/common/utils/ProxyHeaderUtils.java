package com.ds.gateway.common.utils;

import com.ds.gateway.application.security.UserContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for creating headers for proxy requests to downstream services
 */
public class ProxyHeaderUtils {

    /**
     * Create HttpHeaders with X-User-Id and X-User-Roles from UserContext
     * This ensures user ID and roles are extracted from JWT token and forwarded to downstream services
     * Roles are filtered to exclude Keycloak default roles
     */
    public static HttpHeaders createHeadersWithUserId() {
        HttpHeaders headers = new HttpHeaders();
        UserContext.getCurrentUser()
            .ifPresent(user -> {
                headers.set("X-User-Id", user.getUserId());
                
                // Forward roles as comma-separated string
                Set<String> roles = user.getRoles();
                if (roles != null && !roles.isEmpty()) {
                    String rolesHeader = roles.stream()
                        .sorted() // Sort for consistency
                        .collect(Collectors.joining(","));
                    headers.set("X-User-Roles", rolesHeader);
                }
            });
        return headers;
    }

    /**
     * Create HttpHeaders with X-User-Id and X-User-Roles from UserContext and additional headers
     */
    public static HttpHeaders createHeadersWithUserId(HttpHeaders additionalHeaders) {
        HttpHeaders headers = createHeadersWithUserId();
        if (additionalHeaders != null) {
            headers.putAll(additionalHeaders);
        }
        return headers;
    }

    /**
     * Clean response headers by removing Transfer-Encoding to prevent duplicate headers with Nginx
     * This is necessary because RestTemplate forwards Transfer-Encoding from downstream services,
     * and Spring Boot may also add it, causing Nginx to reject the response
     * 
     * @param response The ResponseEntity from RestTemplate
     * @return A new ResponseEntity with cleaned headers
     */
    public static <T> ResponseEntity<T> cleanResponseHeaders(ResponseEntity<T> response) {
        if (response == null) {
            return null;
        }
        
        HttpHeaders cleanedHeaders = new HttpHeaders();
        cleanedHeaders.putAll(response.getHeaders());
        cleanedHeaders.remove("Transfer-Encoding");
        
        return ResponseEntity.status(response.getStatusCode())
            .headers(cleanedHeaders)
            .body(response.getBody());
    }
}
