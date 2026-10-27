package com.ltd.gatewayservice;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class GatewayController {

    private final RestTemplate restTemplate;

    @Value("${services.user-service-url}")
    private String userServiceUrl;

    @Value("${services.order-service-url}")
    private String orderServiceUrl;

    // Gop thong tin user va don hang tra ve trong mot lan goi
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@RequestParam(defaultValue = "1") Long userId) {
        Map<?, ?> user = restTemplate.getForObject(userServiceUrl + "/users/" + userId, Map.class);
        List<?> orders = restTemplate.getForObject(orderServiceUrl + "/orders/user/" + userId, List.class);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Gop du lieu thanh cong");
        response.put("pattern", "Gateway Aggregation");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("data", Map.of(
                "user", user != null ? user : Map.of(),
                "orders", orders != null ? orders : List.of(),
                "totalOrders", orders != null ? orders.size() : 0
        ));

        return ResponseEntity.ok(response);
    }

    // Chuyen tiep yeu cau toi user service
    @PostMapping("/users/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        return restTemplate.postForEntity(userServiceUrl + "/users/login", body, Map.class);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        return restTemplate.getForEntity(userServiceUrl + "/users/" + id, Map.class);
    }

    @PostMapping("/users/{id}/avatar-presigned-url")
    public ResponseEntity<?> getAvatarPresignedUrl(@PathVariable Long id) {
        return restTemplate.postForEntity(userServiceUrl + "/users/" + id + "/avatar-presigned-url", null, Map.class);
    }

    // Chuyen tiep yeu cau toi order service
    @GetMapping("/orders/user/{userId}")
    public ResponseEntity<?> getOrdersByUser(@PathVariable Long userId) {
        return restTemplate.getForEntity(orderServiceUrl + "/orders/user/" + userId, List.class);
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(@RequestParam(required = false) Long userId) {
        String url = orderServiceUrl + "/orders" + (userId != null ? "?userId=" + userId : "");
        return restTemplate.getForEntity(url, List.class);
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> order) {
        return restTemplate.postForEntity(orderServiceUrl + "/orders", order, Map.class);
    }
}
