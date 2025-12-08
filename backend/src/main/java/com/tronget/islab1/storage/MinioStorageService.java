package com.tronget.islab1.storage;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MinioStorageService implements StorageService {

    private final MinioClient client;
    private final String bucket;

    public MinioStorageService(MinioClient client, @Value("${minio.bucket}") String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public String upload(byte[] data, String key, String contentType) throws Exception {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .contentType(contentType == null
                            ? "application/octet-stream"
                            : contentType)
                    .stream(in, data.length, -1)
                    .build());
            return key;
        } catch (MinioException e) {
            throw new RuntimeException("MinIO upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public StorageUploadToken prepareUpload(byte[] data, String finalKey, String contentType) throws Exception {
        String tempKey = finalKey + ".tmp-" + UUID.randomUUID();
        upload(data, tempKey, contentType);
        return new StorageUploadToken(tempKey, finalKey);
    }

    @Override
    public void commitUpload(StorageUploadToken token) {
        try {
            CopySource source = CopySource.builder()
                    .bucket(bucket)
                    .object(token.tempKey())
                    .build();
            client.copyObject(CopyObjectArgs.builder()
                    .bucket(bucket)
                    .object(token.finalKey())
                    .source(source)
                    .build());
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(token.tempKey())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO commit failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void rollbackUpload(StorageUploadToken token) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(token.tempKey())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO rollback failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String key) throws Exception {
        return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
    }

    @Override
    public String presignedUrl(String key, int expirySeconds) throws Exception {
        return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(key)
                .expiry(expirySeconds)
                .build());
    }
}
