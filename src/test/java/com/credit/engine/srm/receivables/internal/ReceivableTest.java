package com.credit.engine.srm.receivables.internal;

import com.credit.engine.srm.shared.AssignorId;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableId;
import com.credit.engine.srm.shared.ReceivableType;
import com.credit.engine.srm.shared.SettlementId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReceivableTest {

    private static final LocalDate REGISTRATION_DATE = LocalDate.of(2026, 1, 15);
    private static final Instant CREATED_AT = Instant.parse("2026-01-15T13:00:00Z");

    @Test
    void shouldCreateAvailableReceivableAndSettleItOnce() {
        Receivable receivable = validReceivable();
        SettlementId settlementId = SettlementId.newId();
        Instant settledAt = CREATED_AT.plusSeconds(60);

        assertThat(receivable.status()).isEqualTo(ReceivableStatus.AVAILABLE);
        assertThat(receivable.settlementId()).isEmpty();

        receivable.markSettled(settlementId, settledAt);

        assertThat(receivable.status()).isEqualTo(ReceivableStatus.SETTLED);
        assertThat(receivable.settlementId()).contains(settlementId);
        assertThat(receivable.settledAt()).contains(settledAt);
        assertThatThrownBy(() -> receivable.markSettled(SettlementId.newId(), settledAt.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already settled");
    }

    @Test
    void shouldRejectInvalidFaceValueAndDueDate() {
        assertThatThrownBy(() -> Receivable.create(
                ReceivableId.newId(),
                AssignorId.newId(),
                ReceivableType.DUPLICATA_MERCANTIL,
                Money.of("100.00", Currency.USD),
                REGISTRATION_DATE.plusMonths(1),
                REGISTRATION_DATE,
                CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BRL");

        assertThatThrownBy(() -> Receivable.create(
                ReceivableId.newId(),
                AssignorId.newId(),
                ReceivableType.DUPLICATA_MERCANTIL,
                Money.of("100.00", Currency.BRL),
                REGISTRATION_DATE,
                REGISTRATION_DATE,
                CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dueDate");

        assertThatThrownBy(() -> Receivable.create(
                ReceivableId.newId(),
                AssignorId.newId(),
                ReceivableType.DUPLICATA_MERCANTIL,
                Money.of("100.00", Currency.BRL),
                REGISTRATION_DATE.plusMonths(360).plusDays(1),
                REGISTRATION_DATE,
                CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("360 months");
    }

    private static Receivable validReceivable() {
        return Receivable.create(
                ReceivableId.newId(),
                AssignorId.newId(),
                ReceivableType.DUPLICATA_MERCANTIL,
                Money.of("100000.00", Currency.BRL),
                REGISTRATION_DATE.plusMonths(3),
                REGISTRATION_DATE,
                CREATED_AT);
    }
}
