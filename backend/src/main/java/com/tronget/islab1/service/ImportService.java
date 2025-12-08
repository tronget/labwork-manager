package com.tronget.islab1.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronget.islab1.dto.ImportOperationDto;
import com.tronget.islab1.dto.LabWorkRequestDto;
import com.tronget.islab1.exceptions.UniqueConstraintViolationException;
import com.tronget.islab1.mappers.LabWorkMapper;
import com.tronget.islab1.models.ImportOperation;
import com.tronget.islab1.models.LabWork;
import com.tronget.islab1.models.UserAccount;
import com.tronget.islab1.storage.StorageService;
import com.tronget.islab1.storage.StorageUploadToken;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ImportService {

    private final ObjectMapper objectMapper;
    private final LabWorkMapper labWorkMapper;
    private final LabWorkService labWorkService;
    private final ImportOperationService importOperationService;
    private final UserAccountService userAccountService;
    private final Validator validator;
    private final TransactionTemplate transactionTemplate;
    private final StorageService storageService;

    public ImportService(ObjectMapper objectMapper,
                         LabWorkMapper labWorkMapper,
                         LabWorkService labWorkService,
                         ImportOperationService importOperationService,
                         UserAccountService userAccountService,
                         Validator validator,
                         PlatformTransactionManager transactionManager,
                         StorageService storageService) {
        this.objectMapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.labWorkMapper = labWorkMapper;
        this.labWorkService = labWorkService;
        this.importOperationService = importOperationService;
        this.userAccountService = userAccountService;
        this.validator = validator;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        this.storageService = storageService;
    }

    public ImportOperationDto importLabWorks(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        byte[] bytes = readAllBytes(file);
        List<LabWorkRequestDto> payload = parse(bytes);
        UserAccount user = userAccountService.getCurrentAccount();
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "import.json";
        ImportOperation operation = importOperationService.start(user, filename, payload.size());
        String objectKey = String.format("imports/%d/%s", operation.getId(), filename);
        long fileSize = bytes.length;
        AtomicReference<StorageUploadToken> tokenRef = new AtomicReference<>(null);
        AtomicReference<Exception> uploadFailure = new AtomicReference<>(null);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                payload.forEach(this::processSingle);
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void beforeCommit(boolean readOnly) {
                                    try {
                                        StorageUploadToken token = storageService.prepareUpload(
                                                bytes, objectKey, file.getContentType());
                                        tokenRef.set(token);
                                    } catch (Exception e) {
                                        uploadFailure.set(e);
                                        throw new RuntimeException("File upload failed: " + e.getMessage(), e);
                                    }
                                }

                                @Override
                                public void afterCompletion(int status) {
                                    StorageUploadToken token = tokenRef.get();
                                    if (token == null) {
                                        return;
                                    }
                                    if (status == TransactionSynchronization.STATUS_COMMITTED) {
                                        try {
                                            storageService.commitUpload(token);
                                            importOperationService.markSuccess(
                                                    operation.getId(), payload.size(), objectKey, fileSize);
                                        } catch (Exception e) {
                                            importOperationService.markFailure(
                                                    operation.getId(), "File commit failed: " + e.getMessage());
                                        }
                                    } else {
                                        storageService.rollbackUpload(token);
                                    }
                                }
                            });
                } else {
                    try {
                        StorageUploadToken token = storageService.prepareUpload(bytes, objectKey, file.getContentType());
                        storageService.commitUpload(token);
                        importOperationService.markSuccess(operation.getId(), payload.size(), objectKey, fileSize);
                    } catch (Exception e) {
                        uploadFailure.set(e);
                        throw new RuntimeException("File upload failed: " + e.getMessage(), e);
                    }
                }
            });
            if (uploadFailure.get() != null) {
                throw new RuntimeException("File upload failed: " +
                        uploadFailure.get().getMessage(),
                        uploadFailure.get());
            }
        } catch (RuntimeException ex) {
            importOperationService.markFailure(operation.getId(), ex.getMessage());
            throw ex;
        }
        ImportOperation refreshed = importOperationService.getOne(operation.getId(), user);
        return importOperationService.toDto(refreshed);
    }

    private void processSingle(LabWorkRequestDto dto) {
        LabWork entity = new LabWork();
        labWorkMapper.setEntityValues(entity, dto);
        validateEntity(entity);
        labWorkService.save(entity);
    }

    private void validateEntity(LabWork entity) {
        Set<ConstraintViolation<LabWork>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            String message = violations.iterator().next().getMessage();
            throw new UniqueConstraintViolationException("validation", message);
        }
    }

    private byte[] readAllBytes(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) {
                throw new IllegalArgumentException("File is empty");
            }
            return bytes;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read file: " + e.getMessage(), e);
        }
    }

    private List<LabWorkRequestDto> parse(byte[] bytes) {
        try {
            return objectMapper.readValue(
                    bytes, new TypeReference<List<LabWorkRequestDto>>() {
                    });
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse file: " + e.getMessage(), e);
        }
    }
}
