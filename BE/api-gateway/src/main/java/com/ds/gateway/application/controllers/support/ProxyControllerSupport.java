package com.ds.gateway.application.controllers.support;

import com.ds.gateway.infrastructure.http.ProxyHttpClient;
import com.ds.gateway.infrastructure.logging.ProxyLogContext;
import com.ds.gateway.infrastructure.logging.ProxyRequestLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Helper component so controllers can keep their code lean while still orchestrating headers/logging.
 */
@Component
@RequiredArgsConstructor
public class ProxyControllerSupport {

    private final ProxyHttpClient proxyHttpClient;
    private final ProxyRequestLogger proxyRequestLogger;

    public ResponseEntity<Object> forward(String targetService, HttpMethod method, String url, Object body) {
        ProxyLogContext context = proxyRequestLogger.start(method, targetService, url, body);
        try {
            ResponseEntity<Object> response = proxyHttpClient.exchange(method, url, body, Object.class);
            proxyRequestLogger.success(context, response.getStatusCode().value());
            return response;
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            proxyRequestLogger.failure(context, 502, ex.getMessage(), ex);
            return ResponseEntity.status(502).body("{\"error\":\"Bad Gateway: service unavailable\"}");
        } catch (HttpStatusCodeException ex) {
            proxyRequestLogger.failure(context, ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex);
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            proxyRequestLogger.failure(context, 500, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body("{\"error\":\"" + ex.getMessage() + "\"}");
        }
    }

    public ResponseEntity<String> forwardForString(String targetService, HttpMethod method, String url, Object body) {
        ProxyLogContext context = proxyRequestLogger.start(method, targetService, url, body);
        try {
            ResponseEntity<String> response = proxyHttpClient.exchange(method, url, body, String.class);
            proxyRequestLogger.success(context, response.getStatusCode().value());
            return response;
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            proxyRequestLogger.failure(context, 502, ex.getMessage(), ex);
            return ResponseEntity.status(502).body("{\"error\":\"Bad Gateway: service unavailable\"}");
        } catch (HttpStatusCodeException ex) {
            proxyRequestLogger.failure(context, ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex);
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            proxyRequestLogger.failure(context, 500, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body("{\"error\":\"" + ex.getMessage() + "\"}");
        }
    }
}
