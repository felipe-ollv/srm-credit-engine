package com.credit.engine.srm.settlements;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SettlementBatchResult(
        UUID batchId,
        String status,
        Instant requestedAt,
        Instant completedAt,
        List<SettlementBatchItemResult> items) {
}
