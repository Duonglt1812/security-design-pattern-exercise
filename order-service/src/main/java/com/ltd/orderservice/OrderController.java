package com.ltd.orderservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// Model don hang
record Order(String id, Long userId, String product, Double amount, String status, String createdAt) {}

@RestController
@RequestMapping("/orders")
public class OrderController {

    // Du lieu don hang gia lap
    private static final Map<String, Order> ORDER_DB = new ConcurrentHashMap<>();

    static {
        ORDER_DB.put("ORD-1001", new Order("ORD-1001", 1L, "Laptop Dell XPS 15", 1850.0, "DELIVERED", "2026-10-20T10:30:00"));
        ORDER_DB.put("ORD-1002", new Order("ORD-1002", 1L, "Chuot Logitech MX Master 3S", 99.0, "PROCESSING", "2026-10-25T14:15:00"));
        ORDER_DB.put("ORD-1003", new Order("ORD-1003", 2L, "Ban phim co Keychron K2", 85.0, "SHIPPED", "2026-10-24T09:00:00"));
    }

    // Lay danh sach don hang
    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestParam(required = false) Long userId) {
        if (userId != null) {
            List<Order> userOrders = ORDER_DB.values().stream()
                    .filter(o -> userId.equals(o.userId()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(userOrders);
        }
        return ResponseEntity.ok(new ArrayList<>(ORDER_DB.values()));
    }

    // Lay danh sach don hang theo user id
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable Long userId) {
        List<Order> userOrders = ORDER_DB.values().stream()
                .filter(o -> userId.equals(o.userId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(userOrders);
    }

    // Lay chi tiet don hang theo ma
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable String id) {
        Order order = ORDER_DB.get(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    // Tao don hang moi
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        String id = (order.id() == null || order.id().isBlank())
                ? "ORD-" + (ORDER_DB.size() + 1001)
                : order.id();
        String createdAt = order.createdAt() != null
                ? order.createdAt()
                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String status = order.status() != null ? order.status() : "CREATED";

        Order saved = new Order(id, order.userId(), order.product(), order.amount(), status, createdAt);
        ORDER_DB.put(id, saved);
        return ResponseEntity.ok(saved);
    }
}
