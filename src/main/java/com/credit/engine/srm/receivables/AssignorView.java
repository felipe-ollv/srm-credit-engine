package com.credit.engine.srm.receivables;

import com.credit.engine.srm.shared.AssignorId;

import java.time.Instant;

public record AssignorView(
        AssignorId id,
        String document,
        String legalName,
        Instant createdAt) {
}
