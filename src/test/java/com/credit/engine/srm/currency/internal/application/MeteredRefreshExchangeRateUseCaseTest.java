package com.credit.engine.srm.currency.internal.application;

import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeteredRefreshExchangeRateUseCaseTest {

    @Test
    void shouldCountSuccessfulRefresh() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Instant now = Instant.parse("2026-08-28T12:00:00Z");
        ExchangeRate rate = new ExchangeRate(
                Currency.USD, Currency.BRL, new BigDecimal("5.4321"), now, now);
        MeteredRefreshExchangeRateUseCase metered = new MeteredRefreshExchangeRateUseCase(
                () -> rate,
                registry);

        assertThat(metered.refreshUsdToBrl()).isSameAs(rate);
        assertThat(registry.get("fx.refresh.total")
                .tag("result", "success").counter().count()).isEqualTo(1);
    }

    @Test
    void shouldCountFailedRefresh() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeteredRefreshExchangeRateUseCase metered = new MeteredRefreshExchangeRateUseCase(
                () -> { throw new IllegalStateException("provider unavailable"); },
                registry);

        assertThatThrownBy(metered::refreshUsdToBrl).isInstanceOf(IllegalStateException.class);
        assertThat(registry.get("fx.refresh.total")
                .tag("result", "error").counter().count()).isEqualTo(1);
    }
}
