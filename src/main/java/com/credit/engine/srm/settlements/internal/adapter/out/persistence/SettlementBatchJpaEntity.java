package com.credit.engine.srm.settlements.internal.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlement_batches")
class SettlementBatchJpaEntity {

    @Id
    UUID id;

    @Column(nullable = false, length = 20)
    String status;

    @Column(name = "requested_at", nullable = false)
    Instant requestedAt;

    @Column(name = "completed_at")
    Instant completedAt;

    protected SettlementBatchJpaEntity() {
    }

    SettlementBatchJpaEntity(UUID id, Instant requestedAt) {
        this.id = id;
        this.status = "PROCESSING";
        this.requestedAt = requestedAt;
    }

    void complete(Instant at) {
        this.status = "COMPLETED";
        this.completedAt = at;
    }
}
