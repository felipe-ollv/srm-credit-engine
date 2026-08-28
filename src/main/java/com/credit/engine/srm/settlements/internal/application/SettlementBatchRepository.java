package com.credit.engine.srm.settlements.internal.application;

import java.time.Instant;
import java.util.UUID;

public interface SettlementBatchRepository {

    void create(UUID batchId, Instant requestedAt);

    void complete(UUID batchId, Instant completedAt);
}
