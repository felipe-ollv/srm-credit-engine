package com.credit.engine.srm.settlements.internal.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlement_idempotency")
class IdempotencyJpaEntity {

    @Id
    @Column(name = "idempotency_key", length = 64)
    String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    String requestHash;

    @Column(name = "batch_id", nullable = false, unique = true)
    UUID batchId;

    @Column(nullable = false, length = 20)
    String status;

    @Column(name = "response_payload", columnDefinition = "text")
    String responsePayload;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "completed_at")
    Instant completedAt;

    protected IdempotencyJpaEntity() {
    }

    IdempotencyJpaEntity(String key, String requestHash, UUID batchId, Instant createdAt) {
        this.key = key;
        this.requestHash = requestHash;
        this.batchId = batchId;
        this.status = "PROCESSING";
        this.createdAt = createdAt;
    }

    void complete(String payload, Instant at) {
        this.status = "COMPLETED";
        this.responsePayload = payload;
        this.completedAt = at;
    }
}
