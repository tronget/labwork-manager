package com.tronget.islab1.service;

import com.tronget.islab1.dto.ImportOperationDto;
import com.tronget.islab1.models.ImportOperation;
import com.tronget.islab1.models.ImportStatus;
import com.tronget.islab1.models.UserAccount;
import com.tronget.islab1.models.UserRole;
import com.tronget.islab1.repository.ImportOperationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportOperationService {

    private final ImportOperationRepository repository;

    public ImportOperationService(ImportOperationRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportOperation start(UserAccount user, String fileName,
                                 int totalRecords) {
        ImportOperation operation = new ImportOperation();
        operation.setUser(user);
        operation.setFileName(fileName);
        operation.setStatus(ImportStatus.STARTED);
        operation.setTotalRecords(totalRecords);
        return repository.save(operation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long operationId, int insertedRecords, String fileKey, Long fileSize) {
        ImportOperation operation =
                repository.findById(operationId)
                        .orElseThrow(()
                                -> new EntityNotFoundException(
                                "Import operation not found"));
        operation.setInsertedRecords(insertedRecords);
        operation.setStatus(ImportStatus.SUCCESS);
        operation.setErrorMessage(null);
        operation.setFileKey(fileKey);
        operation.setFileSize(fileSize);
        repository.save(operation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailure(Long operationId, String message) {
        ImportOperation operation =
                repository.findById(operationId)
                        .orElseThrow(()
                                -> new EntityNotFoundException(
                                "Import operation not found"));
        operation.setStatus(ImportStatus.FAILED);
        operation.setErrorMessage(message);
        repository.save(operation);
    }

    @Transactional(readOnly = true)
    public Page<ImportOperation> findHistory(UserAccount user,
                                             Pageable pageable) {
        if (user.getRole() == UserRole.ADMIN) {
            return repository.findAll(pageable);
        }
        return repository.findAllByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public ImportOperation getOne(Long id, UserAccount requester) {
        ImportOperation operation = repository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Import operation not found"));
        if (requester.getRole() != UserRole.ADMIN &&
                !operation.getUser().getId().equals(requester.getId())) {
            throw new EntityNotFoundException("Import operation not found");
        }
        return operation;
    }

    public ImportOperationDto toDto(ImportOperation operation) {
        return ImportOperationDto.builder()
                .id(operation.getId())
                .status(operation.getStatus())
                .username(operation.getUser().getUsername())
                .fileName(operation.getFileName())
                .fileKey(operation.getFileKey())
                .fileSize(operation.getFileSize())
                .totalRecords(operation.getTotalRecords())
                .insertedRecords(operation.getInsertedRecords())
                .errorMessage(operation.getErrorMessage())
                .createdAt(operation.getCreatedAt())
                .finishedAt(operation.getFinishedAt())
                .build();
    }
}
