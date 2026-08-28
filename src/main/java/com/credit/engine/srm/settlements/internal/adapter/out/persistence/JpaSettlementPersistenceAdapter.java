package com.credit.engine.srm.settlements.internal.adapter.out.persistence;

import com.credit.engine.srm.settlements.internal.Settlement;
import com.credit.engine.srm.settlements.internal.application.IdempotencyRecord;
import com.credit.engine.srm.settlements.internal.application.IdempotencyRepository;
import com.credit.engine.srm.settlements.internal.application.SettlementBatchRepository;
import com.credit.engine.srm.settlements.internal.application.SettlementRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaSettlementPersistenceAdapter implements
        SettlementBatchRepository,
        IdempotencyRepository,
        SettlementRepository {

    private final SpringDataSettlementBatchRepository batches;
    private final SpringDataIdempotencyRepository idempotency;
    private final SpringDataSettlementRepository settlements;

    JpaSettlementPersistenceAdapter(
            SpringDataSettlementBatchRepository batches,
            SpringDataIdempotencyRepository idempotency,
            SpringDataSettlementRepository settlements) {
        this.batches = batches;
        this.idempotency = idempotency;
        this.settlements = settlements;
    }

    @Override
    public void create(UUID batchId, Instant requestedAt) {
        batches.saveAndFlush(new SettlementBatchJpaEntity(batchId, requestedAt));
    }

    @Override
    public void complete(UUID batchId, Instant completedAt) {
        SettlementBatchJpaEntity batch = batches.findById(batchId).orElseThrow();
        batch.complete(completedAt);
        batches.saveAndFlush(batch);
    }

    @Override
    public void create(String key, String requestHash, UUID batchId, Instant createdAt) {
        idempotency.saveAndFlush(new IdempotencyJpaEntity(key, requestHash, batchId, createdAt));
    }

    @Override
    public Optional<IdempotencyRecord> find(String key) {
        return idempotency.findById(key).map(entity -> new IdempotencyRecord(
                entity.key,
                entity.requestHash,
                entity.batchId,
                entity.status,
                entity.responsePayload,
                entity.createdAt,
                entity.completedAt));
    }

    @Override
    public void complete(String key, String responsePayload, Instant completedAt) {
        IdempotencyJpaEntity record = idempotency.findById(key).orElseThrow();
        record.complete(responsePayload, completedAt);
        idempotency.saveAndFlush(record);
    }

    @Override
    public void save(UUID batchId, int itemIndex, Settlement settlement) {
        settlements.saveAndFlush(new SettlementJpaEntity(batchId, itemIndex, settlement));
    }
}
