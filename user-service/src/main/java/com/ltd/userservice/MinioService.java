package com.ltd.userservice;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioService {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.public-endpoint:http://localhost:9000}")
    private String publicEndpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket:avatars}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            this.minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Tao bucket MinIO thanh cong: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("Chua the ket noi MinIO ngay luc khoi dong: {}", e.getMessage());
        }
    }

    // Tao link upload file len MinIO het han sau 5 phut
    public String generatePresignedUploadUrl(String objectName, int expiryMinutes) {
        try {
            try {
                if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                }
            } catch (Exception ignored) {}

            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );

            if (publicEndpoint != null && !publicEndpoint.isBlank() && !publicEndpoint.equals(endpoint)) {
                presignedUrl = presignedUrl.replace(endpoint, publicEndpoint);
            }

            return presignedUrl;
        } catch (Exception e) {
            throw new RuntimeException("Loi tao link presigned: " + e.getMessage(), e);
        }
    }

    public String getBucket() {
        return bucket;
    }
}
