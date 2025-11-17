package com.ds.gateway.infrastructure.http;

import com.ds.gateway.common.utils.ProxyHeaderUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Thin wrapper around RestTemplate that automatically injects the required proxy headers.
 */
@Component
@RequiredArgsConstructor
public class ProxyHttpClient {

    private final RestTemplate restTemplate;

    public <T> ResponseEntity<T> exchange(HttpMethod method, String url, Object body, Class<T> responseType) {
        HttpHeaders headers = ProxyHeaderUtils.createHeadersWithUserId();
        HttpEntity<?> entity = buildEntity(method, headers, body);
        ResponseEntity<T> response = restTemplate.exchange(url, method, entity, responseType);
        return ProxyHeaderUtils.cleanResponseHeaders(response);
    }

    private HttpEntity<?> buildEntity(HttpMethod method, HttpHeaders headers, Object body) {
        if (method == HttpMethod.GET ||
            method == HttpMethod.DELETE ||
            method == HttpMethod.HEAD ||
            method == HttpMethod.OPTIONS) {
            return new HttpEntity<>(headers);
        }
        return new HttpEntity<>(body, headers);
    }
}
