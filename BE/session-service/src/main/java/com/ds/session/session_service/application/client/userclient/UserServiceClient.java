package com.ds.session.session_service.application.client.userclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ds.session.session_service.application.client.userclient.response.DeliveryManResponse;

@FeignClient(
    name = "user-service", 
    url = "${USER_SERVICE_URL:http://localhost:21501}"
)
public interface UserServiceClient {
    @GetMapping("/api/v1/users/{userId}/delivery-man")
    DeliveryManResponse getDeliveryManByUserId(@PathVariable String userId);
}
