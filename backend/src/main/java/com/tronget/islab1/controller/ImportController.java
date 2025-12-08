package com.tronget.islab1.controller;

import com.tronget.islab1.dto.ImportOperationDto;
import com.tronget.islab1.service.ImportOperationService;
import com.tronget.islab1.service.ImportService;
import com.tronget.islab1.service.UserAccountService;
import com.tronget.islab1.storage.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final ImportService importService;
    private final ImportOperationService importOperationService;
    private final UserAccountService userAccountService;
    private final StorageService storageService;

    public ImportController(ImportService importService,
                            ImportOperationService importOperationService,
                            UserAccountService userAccountService,
                            StorageService storageService) {
        this.importService = importService;
        this.importOperationService = importOperationService;
        this.userAccountService = userAccountService;
        this.storageService = storageService;
    }

    @GetMapping
    public ResponseEntity<Page<ImportOperationDto>> history(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        var currentUser = userAccountService.getCurrentAccount();
        Page<ImportOperationDto> dtoPage = importOperationService
                .findHistory(currentUser, pageable)
                .map(importOperationService::toDto);
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping
    public ResponseEntity<ImportOperationDto> importLabworks(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.importLabWorks(file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportOperationDto> getOne(@PathVariable Long id) {
        var currentUser = userAccountService.getCurrentAccount();
        var operation = importOperationService.getOne(id, currentUser);
        return ResponseEntity.ok(importOperationService.toDto(operation));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<?> downloadFile(@PathVariable Long id) {
        var currentUser = userAccountService.getCurrentAccount();
        var operation = importOperationService.getOne(id, currentUser);
        String fileKey = operation.getFileKey();
        if (fileKey == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            var is = storageService.download(fileKey);
            var resource = new org.springframework.core.io.InputStreamResource(is);
            var builder = ResponseEntity.ok().header(
                    "Content-Disposition",
                    "attachment; filename=\"" + operation.getFileName() + "\""
            );
            if (operation.getFileSize() != null) {
                builder.header("Content-Length", String.valueOf(operation.getFileSize()));
            }
            return builder.body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to download file: " + e.getMessage());
        }
    }
}
