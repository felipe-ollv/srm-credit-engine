package com.credit.engine.srm.shared;

import java.util.Objects;
import java.util.UUID;

public record ReceivableId(UUID value) {

    public ReceivableId {
        Objects.requireNonNull(value, "value is required");
    }

    public static ReceivableId newId() {
        return new ReceivableId(UUID.randomUUID());
    }
}
