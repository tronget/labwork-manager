package com.tronget.islab1.storage;

import java.io.InputStream;

public interface StorageService {
    /**
     * One-phase upload (non-transactional). Kept for backward compatibility.
     */
    String upload(byte[] data, String key, String contentType) throws Exception;

    InputStream download(String key) throws Exception;

    String presignedUrl(String key, int expirySeconds) throws Exception;

    /**
     * Prepare part of two-phase commit: upload to a temporary object and return a token.
     */
    StorageUploadToken prepareUpload(byte[] data, String finalKey, String contentType) throws Exception;

    /**
     * Commit phase: finalize prepared upload (e.g., rename/copy temp to final) and cleanup temp artifacts.
     */
    void commitUpload(StorageUploadToken token) throws Exception;

    /**
     * Rollback phase: remove any prepared artifacts.
     */
    void rollbackUpload(StorageUploadToken token);
}
