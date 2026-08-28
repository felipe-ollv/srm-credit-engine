package com.credit.engine.srm.settlements.internal.application;

import java.time.Instant;
import java.util.UUID;

public record IdempotencyRecord(
        String key,
        String requestHash,
        UUID batchId,
        String status,
        String responsePayload,
        Instant createdAt,
        Instant completedAt) {
}
