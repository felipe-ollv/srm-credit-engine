package com.credit.engine.srm.receivables.internal.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignors")
class AssignorJpaEntity {

    @Id
    UUID id;

    @Column(nullable = false, length = 14, unique = true)
    String document;

    @Column(name = "legal_name", nullable = false, length = 160)
    String legalName;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected AssignorJpaEntity() {
    }

    AssignorJpaEntity(UUID id, String document, String legalName, Instant createdAt) {
        this.id = id;
        this.document = document;
        this.legalName = legalName;
        this.createdAt = createdAt;
    }
}
