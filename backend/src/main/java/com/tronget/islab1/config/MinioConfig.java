package com.tronget.islab1.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(@Value("${minio.endpoint}") String endpoint,
                                   @Value("${minio.access-key}") String accessKey,
                                   @Value("${minio.secret-key}") String secretKey,
                                   @Value("${minio.bucket}") String bucket,
                                   @Value("${minio.bucket-check-enabled:true}")
                                   boolean bucketCheckEnabled) {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        if (!bucketCheckEnabled) {
            return client;
        }
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure MinIO bucket: " + e.getMessage(), e);
        }
        return client;
    }
}
