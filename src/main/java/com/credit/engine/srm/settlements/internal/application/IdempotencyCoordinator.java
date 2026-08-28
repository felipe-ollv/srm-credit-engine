package com.credit.engine.srm.settlements.internal.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyCoordinator {

    private final SettlementBatchRepository batches;
    private final IdempotencyRepository idempotency;

    IdempotencyCoordinator(
            SettlementBatchRepository batches,
            IdempotencyRepository idempotency) {
        this.batches = batches;
        this.idempotency = idempotency;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void claim(String key, String requestHash, UUID batchId, Instant requestedAt) {
        batches.create(batchId, requestedAt);
        idempotency.create(key, requestHash, batchId, requestedAt);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyRecord> find(String key) {
        return idempotency.find(key);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(
            String key,
            UUID batchId,
            String responsePayload,
            Instant completedAt) {
        batches.complete(batchId, completedAt);
        idempotency.complete(key, responsePayload, completedAt);
    }
}
