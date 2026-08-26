package com.credit.engine.srm.settlements.internal;

import com.credit.engine.srm.pricing.PricingResult;
import com.credit.engine.srm.pricing.Term;
import com.credit.engine.srm.shared.AssignorId;
import com.credit.engine.srm.shared.ReceivableId;
import com.credit.engine.srm.shared.ReceivableType;
import com.credit.engine.srm.shared.SettlementId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record Settlement(
        SettlementId id,
        ReceivableId receivableId,
        AssignorId assignorId,
        ReceivableType receivableType,
        LocalDate dueDate,
        PricingResult pricingResult,
        Instant settledAt) {

    public Settlement {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(receivableId, "receivableId is required");
        Objects.requireNonNull(assignorId, "assignorId is required");
        Objects.requireNonNull(receivableType, "receivableType is required");
        Objects.requireNonNull(dueDate, "dueDate is required");
        Objects.requireNonNull(pricingResult, "pricingResult is required");
        Objects.requireNonNull(settledAt, "settledAt is required");

        if (receivableType != pricingResult.receivableType()) {
            throw new IllegalArgumentException("settlement type must match pricing result type");
        }
        if (!Term.between(pricingResult.pricingDate(), dueDate).equals(pricingResult.term())) {
            throw new IllegalArgumentException("settlement dueDate must match the priced term");
        }
        if (settledAt.isBefore(pricingResult.calculatedAt())) {
            throw new IllegalArgumentException("settledAt cannot be before pricing calculation");
        }
    }

    public static Settlement create(
            SettlementId id,
            ReceivableId receivableId,
            AssignorId assignorId,
            ReceivableType receivableType,
            LocalDate dueDate,
            PricingResult pricingResult,
            Instant settledAt) {

        return new Settlement(
                id,
                receivableId,
                assignorId,
                receivableType,
                dueDate,
                pricingResult,
                settledAt);
    }
}
