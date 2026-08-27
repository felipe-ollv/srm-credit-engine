package com.credit.engine.srm.receivables.internal;

import com.credit.engine.srm.shared.AssignorId;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableId;
import com.credit.engine.srm.shared.ReceivableType;
import com.credit.engine.srm.shared.SettlementId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public final class Receivable {

    private final ReceivableId id;
    private final AssignorId assignorId;
    private final ReceivableType type;
    private final Money faceValue;
    private final LocalDate dueDate;
    private final LocalDate registrationDate;
    private final Instant createdAt;

    private ReceivableStatus status;
    private SettlementId settlementId;
    private Instant settledAt;

    private Receivable(
            ReceivableId id,
            AssignorId assignorId,
            ReceivableType type,
            Money faceValue,
            LocalDate dueDate,
            LocalDate registrationDate,
            Instant createdAt) {

        this.id = Objects.requireNonNull(id, "id is required");
        this.assignorId = Objects.requireNonNull(assignorId, "assignorId is required");
        this.type = Objects.requireNonNull(type, "type is required");
        this.faceValue = Objects.requireNonNull(faceValue, "faceValue is required");
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate is required");
        this.registrationDate = Objects.requireNonNull(registrationDate, "registrationDate is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");

        if (faceValue.currency() != Currency.BRL) {
            throw new IllegalArgumentException("receivable faceValue must be denominated in BRL");
        }
        if (!faceValue.isPositive()) {
            throw new IllegalArgumentException("receivable faceValue must be positive");
        }
        if (!dueDate.isAfter(registrationDate)) {
            throw new IllegalArgumentException("dueDate must be after registrationDate");
        }
        if (dueDate.isAfter(registrationDate.plusMonths(360))) {
            throw new IllegalArgumentException("dueDate cannot exceed 360 months from registrationDate");
        }

        this.status = ReceivableStatus.AVAILABLE;
    }

    public static Receivable create(
            ReceivableId id,
            AssignorId assignorId,
            ReceivableType type,
            Money faceValue,
            LocalDate dueDate,
            LocalDate registrationDate,
            Instant createdAt) {

        return new Receivable(
                id, assignorId, type, faceValue, dueDate, registrationDate, createdAt);
    }

    public void markSettled(SettlementId settlementId, Instant settledAt) {
        Objects.requireNonNull(settlementId, "settlementId is required");
        Objects.requireNonNull(settledAt, "settledAt is required");

        if (status == ReceivableStatus.SETTLED) {
            throw new IllegalStateException("receivable is already settled");
        }
        if (settledAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("settledAt cannot be before createdAt");
        }

        this.status = ReceivableStatus.SETTLED;
        this.settlementId = settlementId;
        this.settledAt = settledAt;
    }

    public ReceivableId id() {
        return id;
    }

    public AssignorId assignorId() {
        return assignorId;
    }

    public ReceivableType type() {
        return type;
    }

    public Money faceValue() {
        return faceValue;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public LocalDate registrationDate() {
        return registrationDate;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public ReceivableStatus status() {
        return status;
    }

    public Optional<SettlementId> settlementId() {
        return Optional.ofNullable(settlementId);
    }

    public Optional<Instant> settledAt() {
        return Optional.ofNullable(settledAt);
    }
}
