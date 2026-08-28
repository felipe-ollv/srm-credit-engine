package com.credit.engine.srm.settlements.internal.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository {

    void create(String key, String requestHash, UUID batchId, Instant createdAt);

    Optional<IdempotencyRecord> find(String key);

    void complete(String key, String responsePayload, Instant completedAt);
}
