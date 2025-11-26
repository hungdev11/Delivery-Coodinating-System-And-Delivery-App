package com.ds.gateway.application.filters;

import com.ds.gateway.application.security.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Filter to log all incoming requests to the API Gateway.
 * This is the ONLY place where log.info() is allowed for request logging.
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10) // Run after Security to get UserContext, but before other logic
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Capture request details
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = query != null ? path + "?" + query : path;

        // Extract headers (sanitized)
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // Skip sensitive headers
            if (!isSensitiveHeader(headerName)) {
                headers.put(headerName, request.getHeader(headerName));
            } else {
                headers.put(headerName, "***");
            }
        }

        // Get User ID if available (requires this filter to run after
        // SecurityContextPersistenceFilter)
        String userId = UserContext.getCurrentUser()
                .map(UserContext::getUserId)
                .orElse("anonymous");

        // Log the request
        log.info("[api-gateway] [RequestLoggingFilter.logRequest] {} {} - User: {} - Headers: {}",
                method, fullPath, userId, headers);

        // Proceed
        filterChain.doFilter(request, response);
    }

    private boolean isSensitiveHeader(String headerName) {
        String lower = headerName.toLowerCase();
        return lower.contains("authorization") ||
                lower.contains("cookie") ||
                lower.contains("token") ||
                lower.contains("secret") ||
                lower.contains("password") ||
                lower.contains("key");
    }
}
