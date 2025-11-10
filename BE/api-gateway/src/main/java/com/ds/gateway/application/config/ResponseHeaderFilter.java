package com.ds.gateway.application.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Filter to fix double chunked encoding issue with Cloudflare Tunnel
 * Wraps response to buffer content and calculate Content-Length
 */
@Slf4j
// @Component  // Temporarily disabled - Nginx will handle response buffering
@Order(1)
public class ResponseHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Use ContentCachingResponseWrapper to buffer the response
        // This allows Spring to calculate Content-Length instead of using chunked encoding
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        
        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            // After response is committed, remove Transfer-Encoding if Content-Length is set
            // This prevents double chunked encoding
            if (wrappedResponse.getContentSize() > 0 && wrappedResponse.isCommitted()) {
                // ContentCachingResponseWrapper should have set Content-Length
                // Remove any Transfer-Encoding header to prevent conflicts
                if (response.containsHeader("Transfer-Encoding")) {
                    log.debug("Removing Transfer-Encoding header to prevent double chunked encoding");
                    response.setHeader("Transfer-Encoding", null);
                }
            }
            // Copy the cached content to the actual response
            wrappedResponse.copyBodyToResponse();
        }
    }
}
