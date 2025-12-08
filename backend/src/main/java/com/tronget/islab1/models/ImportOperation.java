package com.tronget.islab1.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ImportOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ImportStatus status = ImportStatus.STARTED;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private int totalRecords = 0;

    @Column(nullable = false)
    private int insertedRecords = 0;

    private String errorMessage;

    private String fileKey;

    private Long fileSize;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant finishedAt;
}
