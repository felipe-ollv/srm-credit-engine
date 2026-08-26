package com.credit.engine.srm.shared;

import java.util.Objects;
import java.util.UUID;

public record SettlementId(UUID value) {

    public SettlementId {
        Objects.requireNonNull(value, "value is required");
    }

    public static SettlementId newId() {
        return new SettlementId(UUID.randomUUID());
    }
}
