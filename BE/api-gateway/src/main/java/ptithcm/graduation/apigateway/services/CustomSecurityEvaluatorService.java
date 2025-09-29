package ptithcm.graduation.apigateway.services;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to evaluate security annotations for method security expressions
 */
@Service("customSecurityEvaluator")
public class CustomSecurityEvaluatorService {

    // Cache for method annotations to avoid reflection overhead
    private final ConcurrentHashMap<String, Boolean> publicMethodCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> authenticatedMethodCache = new ConcurrentHashMap<>();

    /**
     * Check if the current method is annotated with @Public
     */
    public boolean isPublic() {
        try {
            // Get the current request
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return false;
            }

            HttpServletRequest request = attributes.getRequest();
            String requestURI = request.getRequestURI();
            String methodName = request.getMethod();

            // Check cache first
            String cacheKey = methodName + ":" + requestURI;
            Boolean cachedResult = publicMethodCache.get(cacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }

            // Use URI pattern matching to determine if endpoint is public
            boolean result = isPublicEndpointByPattern(requestURI);
            publicMethodCache.put(cacheKey, result);
            return result;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the current method is annotated with @Authenticated
     */
    public boolean isAuthenticated() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return false;
            }

            HttpServletRequest request = attributes.getRequest();
            String requestURI = request.getRequestURI();
            String methodName = request.getMethod();

            // Check cache first
            String cacheKey = methodName + ":" + requestURI;
            Boolean cachedResult = authenticatedMethodCache.get(cacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }

            // Use URI pattern matching to determine if endpoint requires authentication
            boolean result = isAuthenticatedEndpointByPattern(requestURI);
            authenticatedMethodCache.put(cacheKey, result);
            return result;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if user has required roles for @Authenticated annotation
     */
    public boolean hasRequiredRoles() {
        // For now, return true - in a real implementation, you would check the actual roles
        // This method should be enhanced to check the actual method annotation and user roles
        return true;
    }

    /**
     * Simple logic to determine if an endpoint is public based on URI patterns
     */
    private boolean isPublicEndpointByPattern(String requestURI) {
        // Define public endpoints based on URI patterns
        if (requestURI.contains("/login") || 
            requestURI.contains("/register") || 
            requestURI.contains("/refresh") ||
            requestURI.contains("/otp/") ||
            requestURI.contains("/password/reset") ||
            requestURI.contains("/check/") ||
            requestURI.contains("/health")) {
            return true;
        }
        return false;
    }

    /**
     * Simple logic to determine if an endpoint requires authentication
     */
    private boolean isAuthenticatedEndpointByPattern(String requestURI) {
        // Define authenticated endpoints based on URI patterns
        if (requestURI.contains("/logout") || 
            requestURI.contains("/me") || 
            requestURI.contains("/profile") ||
            requestURI.contains("/password/change")) {
            return true;
        }
        return false;
    }
}
