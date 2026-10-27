package com.ltd.userservice;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Cac model du lieu
record User(Long id, String username, String fullName, String email, String role, String avatarUrl) {}
record LoginRequest(String username, String password) {}
record AuthResponse(String token, String tokenType, Long userId, String username, String role, long expiresInMs) {}
record PresignedUrlResponse(String uploadUrl, String objectName, String bucket, int expiresInSeconds, String httpMethod, String patternName, String description) {}

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final JwtService jwtService;
    private final MinioService minioService;

    // Du lieu gia lap
    private static final Map<Long, User> USER_DB = new ConcurrentHashMap<>();

    static {
        USER_DB.put(1L, new User(
                1L, "cuongtm", "ThS. Tran Manh Cuong",
                "cuongtm@university.edu.vn", "ROLE_USER",
                "http://localhost:9000/avatars/user_1_avatar.png"));

        USER_DB.put(2L, new User(
                2L, "user2", "Nguyen Van A",
                "nguyenvana@example.com", "ROLE_USER",
                "http://localhost:9000/avatars/user_2_avatar.png"));
    }

    // 1. Dang nhap va tao token
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.username() == null || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui long nhap username va password"));
        }

        Long userId = 1L;
        String role = "ROLE_USER";
        if ("admin".equalsIgnoreCase(request.username())) {
            userId = 999L;
            role = "ROLE_ADMIN";
        } else if ("user2".equalsIgnoreCase(request.username())) {
            userId = 2L;
        }

        String token = jwtService.generateToken(userId, request.username(), role);
        AuthResponse response = new AuthResponse(token, "Bearer", userId, request.username(), role, 86400000);
        return ResponseEntity.ok(response);
    }

    // 2. Lay thong tin user theo id
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User user = USER_DB.get(id);
        if (user == null) {
            user = new User(id, "user" + id, "User " + id, "user" + id + "@example.com", "ROLE_USER",
                    "http://localhost:9000/avatars/user_" + id + "_avatar.png");
        }
        return ResponseEntity.ok(user);
    }

    // 3. Valet Key tao link upload avatar het han sau 5 phut
    @PostMapping("/{id}/avatar-presigned-url")
    public ResponseEntity<PresignedUrlResponse> generateAvatarUploadUrl(@PathVariable Long id) {
        String objectName = "user_" + id + "_avatar.png";
        int expiryMinutes = 5;

        String presignedUrl = minioService.generatePresignedUploadUrl(objectName, expiryMinutes);
        PresignedUrlResponse response = new PresignedUrlResponse(
                presignedUrl,
                objectName,
                minioService.getBucket(),
                expiryMinutes * 60,
                "PUT",
                "Valet Key",
                "Upload avatar truc tiep len MinIO trong vong 5 phut"
        );
        return ResponseEntity.ok(response);
    }

    // 4. Kiem tra tinh hop le cua token
    @GetMapping("/auth/verify")
    public ResponseEntity<Void> verifyToken(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7).trim();
        if (token.isEmpty() || !jwtService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Claims claims = jwtService.extractClaims(token);
        return ResponseEntity.ok()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Name", claims.get("username", String.class))
                .header("X-User-Role", claims.get("role", String.class))
                .build();
    }
}
