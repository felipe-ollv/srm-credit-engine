package com.credit.engine.srm.settlements.internal.application;

import com.credit.engine.srm.settlements.CreateSettlementBatchUseCase;
import com.credit.engine.srm.settlements.SettlementBatchCommand;
import com.credit.engine.srm.settlements.SettlementBatchItemResult;
import com.credit.engine.srm.settlements.SettlementBatchResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Primary
public final class MeteredSettlementBatchUseCase implements CreateSettlementBatchUseCase {

    private static final String DURATION_METRIC = "settlement.batch.duration";
    private static final String ITEM_METRIC = "settlement.items.total";

    private final CreateSettlementBatchUseCase delegate;
    private final MeterRegistry meterRegistry;

    MeteredSettlementBatchUseCase(
            @Qualifier("settlementBatchService") CreateSettlementBatchUseCase delegate,
            MeterRegistry meterRegistry) {
        this.delegate = Objects.requireNonNull(delegate, "delegate is required");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry is required");
    }

    @Override
    public SettlementBatchResult create(String idempotencyKey, SettlementBatchCommand command) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            SettlementBatchResult response = delegate.create(idempotencyKey, command);
            response.items().forEach(this::recordItem);
            return response;
        } catch (RuntimeException exception) {
            result = "error";
            throw exception;
        } finally {
            sample.stop(Timer.builder(DURATION_METRIC)
                    .description("Settlement batch processing latency")
                    .tag("result", result)
                    .register(meterRegistry));
        }
    }

    private void recordItem(SettlementBatchItemResult item) {
        Counter.builder(ITEM_METRIC)
                .description("Processed settlement items")
                .tag("result", item.status().name())
                .tag("currency", item.paymentCurrency())
                .register(meterRegistry)
                .increment();
    }
}
