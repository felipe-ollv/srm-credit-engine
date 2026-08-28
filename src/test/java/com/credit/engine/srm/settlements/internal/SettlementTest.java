package com.credit.engine.srm.settlements.internal;

import com.credit.engine.srm.pricing.InterestRate;
import com.credit.engine.srm.pricing.PricingEngine;
import com.credit.engine.srm.pricing.PricingRequest;
import com.credit.engine.srm.pricing.PricingResult;
import com.credit.engine.srm.shared.AssignorId;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableId;
import com.credit.engine.srm.shared.ReceivableType;
import com.credit.engine.srm.shared.SettlementId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementTest {

    private static final LocalDate PRICING_DATE = LocalDate.of(2026, 1, 15);
    private static final LocalDate DUE_DATE = PRICING_DATE.plusMonths(3);
    private static final Instant CALCULATED_AT = Instant.parse("2026-01-15T13:00:00Z");

    @Test
    void shouldCreateImmutableSettlementWithCompleteSnapshot() {
        PricingResult pricingResult = pricingResult(ReceivableType.DUPLICATA_MERCANTIL);
        SettlementId settlementId = SettlementId.newId();
        ReceivableId receivableId = ReceivableId.newId();
        AssignorId assignorId = AssignorId.newId();
        Instant settledAt = CALCULATED_AT.plusSeconds(30);

        Settlement settlement = Settlement.create(
                settlementId,
                receivableId,
                assignorId,
                "12345678000195",
                "Assignor Test Ltda.",
                ReceivableType.DUPLICATA_MERCANTIL,
                DUE_DATE,
                pricingResult,
                settledAt);

        assertThat(settlement.id()).isEqualTo(settlementId);
        assertThat(settlement.receivableId()).isEqualTo(receivableId);
        assertThat(settlement.assignorId()).isEqualTo(assignorId);
        assertThat(settlement.assignorDocument()).isEqualTo("12345678000195");
        assertThat(settlement.assignorLegalName()).isEqualTo("Assignor Test Ltda.");
        assertThat(settlement.dueDate()).isEqualTo(DUE_DATE);
        assertThat(settlement.pricingResult()).isEqualTo(pricingResult);
        assertThat(settlement.settledAt()).isEqualTo(settledAt);
    }

    @Test
    void shouldRejectTypeMismatchAndSettlementBeforePricing() {
        PricingResult pricingResult = pricingResult(ReceivableType.DUPLICATA_MERCANTIL);

        assertThatThrownBy(() -> Settlement.create(
                SettlementId.newId(),
                ReceivableId.newId(),
                AssignorId.newId(),
                "12345678000195",
                "Assignor Test Ltda.",
                ReceivableType.CHEQUE_PRE_DATADO,
                DUE_DATE,
                pricingResult,
                CALCULATED_AT.plusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");

        assertThatThrownBy(() -> Settlement.create(
                SettlementId.newId(),
                ReceivableId.newId(),
                AssignorId.newId(),
                "12345678000195",
                "Assignor Test Ltda.",
                ReceivableType.DUPLICATA_MERCANTIL,
                DUE_DATE,
                pricingResult,
                CALCULATED_AT.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before");
    }

    @Test
    void shouldRejectDueDateThatDoesNotMatchPricedTerm() {
        PricingResult pricingResult = pricingResult(ReceivableType.DUPLICATA_MERCANTIL);

        assertThatThrownBy(() -> Settlement.create(
                SettlementId.newId(),
                ReceivableId.newId(),
                AssignorId.newId(),
                "12345678000195",
                "Assignor Test Ltda.",
                ReceivableType.DUPLICATA_MERCANTIL,
                DUE_DATE.plusMonths(1),
                pricingResult,
                CALCULATED_AT.plusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("priced term");
    }

    private static PricingResult pricingResult(ReceivableType type) {
        return PricingEngine.standard().price(new PricingRequest(
                type,
                Money.of("100000.00", Currency.BRL),
                PRICING_DATE,
                DUE_DATE,
                InterestRate.of("0.01"),
                Currency.BRL,
                Optional.empty(),
                CALCULATED_AT));
    }
}
