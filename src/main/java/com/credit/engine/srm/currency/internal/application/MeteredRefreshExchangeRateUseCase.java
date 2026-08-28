package com.credit.engine.srm.currency.internal.application;

import com.credit.engine.srm.currency.RefreshExchangeRateUseCase;
import com.credit.engine.srm.shared.ExchangeRate;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Primary
public final class MeteredRefreshExchangeRateUseCase implements RefreshExchangeRateUseCase {

    private static final String METRIC_NAME = "fx.refresh.total";

    private final RefreshExchangeRateUseCase delegate;
    private final MeterRegistry meterRegistry;

    MeteredRefreshExchangeRateUseCase(
            @Qualifier("currencyService") RefreshExchangeRateUseCase delegate,
            MeterRegistry meterRegistry) {
        this.delegate = Objects.requireNonNull(delegate, "delegate is required");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry is required");
    }

    @Override
    public ExchangeRate refreshUsdToBrl() {
        String result = "success";
        try {
            return delegate.refreshUsdToBrl();
        } catch (RuntimeException exception) {
            result = "error";
            throw exception;
        } finally {
            Counter.builder(METRIC_NAME)
                    .description("Exchange rate refresh attempts")
                    .tag("result", result)
                    .register(meterRegistry)
                    .increment();
        }
    }
}
