package com.credit.engine.srm.settlements.internal.application;

import com.credit.engine.srm.settlements.SettlementBatchCommand;
import com.credit.engine.srm.settlements.SettlementBatchItemResult;
import com.credit.engine.srm.settlements.SettlementBatchResult;
import com.credit.engine.srm.settlements.SettlementItemStatus;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ReceivableId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeteredSettlementBatchUseCaseTest {

    @Test
    void shouldRecordBatchLatencyAndEveryItemResult() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SettlementBatchCommand command = command(Currency.BRL);
        SettlementBatchResult expected = new SettlementBatchResult(
                UUID.randomUUID(),
                "COMPLETED",
                Instant.parse("2026-08-28T12:00:00Z"),
                Instant.parse("2026-08-28T12:00:01Z"),
                List.of(SettlementBatchItemResult.failure(
                        command.items().getFirst().receivableId().value(),
                        "BRL",
                        SettlementItemStatus.CONFLICT,
                        "RECEIVABLE_ALREADY_SETTLED",
                        "Already settled")));
        MeteredSettlementBatchUseCase metered = new MeteredSettlementBatchUseCase(
                (ignoredKey, ignoredCommand) -> expected,
                registry);

        assertThat(metered.create("batch-001", command)).isSameAs(expected);
        assertThat(registry.get("settlement.batch.duration")
                .tag("result", "success").timer().count()).isOne();
        assertThat(registry.get("settlement.items.total")
                .tags("result", "CONFLICT", "currency", "BRL").counter().count()).isEqualTo(1);
    }

    @Test
    void shouldRecordEnvelopeFailureWithoutCountingItems() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SettlementBatchCommand command = command(Currency.USD);
        MeteredSettlementBatchUseCase metered = new MeteredSettlementBatchUseCase(
                (ignoredKey, ignoredCommand) -> { throw new IllegalStateException("failure"); },
                registry);

        assertThatThrownBy(() -> metered.create("batch-002", command))
                .isInstanceOf(IllegalStateException.class);
        assertThat(registry.get("settlement.batch.duration")
                .tag("result", "error").timer().count()).isOne();
        assertThat(registry.find("settlement.items.total").counters()).isEmpty();
    }

    private static SettlementBatchCommand command(Currency currency) {
        return new SettlementBatchCommand(List.of(new SettlementBatchCommand.Item(
                new ReceivableId(UUID.randomUUID()), currency)));
    }
}
