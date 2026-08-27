package com.credit.engine.srm.receivables.internal;

import com.credit.engine.srm.shared.AssignorId;

import java.time.Instant;
import java.util.Objects;

public final class Assignor {

    private final AssignorId id;
    private final Cnpj document;
    private final String legalName;
    private final Instant createdAt;

    private Assignor(AssignorId id, Cnpj document, String legalName, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.document = Objects.requireNonNull(document, "document is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        String normalizedName = Objects.requireNonNull(legalName, "legalName is required").trim();
        if (normalizedName.length() < 2 || normalizedName.length() > 160) {
            throw new IllegalArgumentException("legalName must contain between 2 and 160 characters");
        }
        if (normalizedName.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("legalName must not contain control characters");
        }
        this.legalName = normalizedName;
    }

    public static Assignor create(AssignorId id, String document, String legalName, Instant createdAt) {
        return new Assignor(id, Cnpj.of(document), legalName, createdAt);
    }

    public AssignorId id() { return id; }
    public String document() { return document.value(); }
    public String legalName() { return legalName; }
    public Instant createdAt() { return createdAt; }
}
