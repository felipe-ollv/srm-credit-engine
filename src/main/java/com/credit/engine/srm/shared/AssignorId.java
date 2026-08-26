package com.credit.engine.srm.shared;

import java.util.Objects;
import java.util.UUID;

public record AssignorId(UUID value) {

    public AssignorId {
        Objects.requireNonNull(value, "value is required");
    }

    public static AssignorId newId() {
        return new AssignorId(UUID.randomUUID());
    }
}
