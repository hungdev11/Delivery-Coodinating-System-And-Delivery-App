package com.ds.gateway.application.controllers.v1;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/parcels")
@RequiredArgsConstructor
public class ParcelServiceController {

    private final RestTemplate restTemplate;

    @Value("${services.parcel.base-url}")
    private String baseUrl;

    private String PARCEL_URL;

    @PostConstruct
    private void init() {
        this.PARCEL_URL = baseUrl + "/api/v1/parcels";
    }

    @PostMapping
    public ResponseEntity<?> createParcel(@RequestBody Object request) {
        return ResponseEntity.ok(restTemplate.postForObject(PARCEL_URL, request, Object.class));
    }

    @PutMapping("/{parcelId}")
    public ResponseEntity<?> updateParcel(
            @PathVariable UUID parcelId,
            @RequestBody Object request) {
        restTemplate.put(PARCEL_URL+"/"+parcelId.toString(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{parcelId}")
    public ResponseEntity<?> getParcelById(@PathVariable UUID parcelId) {
        return ResponseEntity.ok(restTemplate.getForObject(PARCEL_URL+"/"+parcelId.toString(), Object.class));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<?> getParcelByCode(@PathVariable String code) {
        return ResponseEntity.ok(restTemplate.getForObject(PARCEL_URL+"/code/"+code, Object.class));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getParcelsSent(@RequestParam String customerId, @RequestParam int page, @RequestParam int size) {

        String finalUrl = UriComponentsBuilder.fromUriString(PARCEL_URL)
                .path("/me")
                .queryParam("customerId", customerId)
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                finalUrl, 
                HttpMethod.GET, 
                null, // null for no request body
                Object.class
            );
            return response;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error during proxied get parcels receive: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @GetMapping("/me/receive")
    public ResponseEntity<?> getParcelsReceive(@RequestParam String customerId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

        String finalUrl = UriComponentsBuilder.fromUriString(PARCEL_URL)
                .path("/me/receive")
                .queryParam("customerId", customerId)
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                finalUrl, 
                HttpMethod.GET, 
                null, // null for no request body
                Object.class
            );
            return response;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error during proxied get parcels receive: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
    
    @GetMapping()
    public ResponseEntity<?> getParcels(
            String status,
            String deliveryType,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String direction) {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        if (status != null) {
            params.add("status", status);
        }
        if (deliveryType != null) {
            params.add("deliveryType", deliveryType);
        }
        if (createdFrom != null) {
            params.add("createdFrom", createdFrom.toString());
        }
        if (createdTo != null) {
            params.add("createdTo", createdTo.toString());
        }

        params.add("page", String.valueOf(page));
        params.add("size", String.valueOf(size));

        if (sortBy != null) {
            params.add("sortBy", sortBy);
        }

        params.add("direction", direction);

        String finalUrl = UriComponentsBuilder.fromUriString(PARCEL_URL)
                .queryParams(params)
                .toUriString();

        log.info(finalUrl);
        Object response = restTemplate.getForObject(finalUrl, Object.class);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{parcelId}")
    public ResponseEntity<Void> deleteParcel(@PathVariable UUID parcelId) {
        return ResponseEntity.ok(null);
    }

    @PutMapping("/change-status/{parcelId}")
    public ResponseEntity<Object> changeParcelStatus(
            @PathVariable String parcelId,
            @RequestParam String event
    ) {
        String finalUrl = UriComponentsBuilder.fromUriString(PARCEL_URL)
                .path("/change-status/{parcelId}")
                .queryParam("event", event)
                .buildAndExpand(parcelId) // Map the {parcelId} path variable
                .toUriString();

        log.info("Proxying PUT request to Parcel Service: {}", finalUrl);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                finalUrl, 
                HttpMethod.PUT, 
                null, // null for no request body
                Object.class
            );
            return response;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error during proxied change status call: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}
