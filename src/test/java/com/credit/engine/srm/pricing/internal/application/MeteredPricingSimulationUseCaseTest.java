package com.credit.engine.srm.pricing.internal.application;

import com.credit.engine.srm.pricing.InterestRate;
import com.credit.engine.srm.pricing.PricingSimulationCommand;
import com.credit.engine.srm.pricing.PricingSimulationResult;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeteredPricingSimulationUseCaseTest {

    @Test
    void shouldRecordLatencyByCurrencyAndResult() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PricingSimulationResult expected = new PricingSimulationResult(
                ReceivableType.DUPLICATA_MERCANTIL,
                Money.of("100.00", Currency.BRL),
                Money.of("90.00", Currency.BRL),
                Money.of("10.00", Currency.BRL),
                Money.of("90.00", Currency.BRL),
                1,
                InterestRate.of("0.01"),
                InterestRate.of("0.015"),
                Optional.empty(),
                LocalDate.of(2026, 8, 26),
                Instant.parse("2026-08-26T19:00:00Z"));
        MeteredPricingSimulationUseCase metered = new MeteredPricingSimulationUseCase(
                command -> expected,
                registry);

        PricingSimulationResult result = metered.simulate(new PricingSimulationCommand(
                ReceivableType.DUPLICATA_MERCANTIL,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 9, 26),
                Currency.BRL));

        assertThat(result).isEqualTo(expected);
        assertThat(registry.get("pricing.simulation.duration")
                .tags("currency", "BRL", "result", "success")
                .timer()
                .count()).isOne();
    }

    @Test
    void shouldTagFailedSimulation() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeteredPricingSimulationUseCase metered = new MeteredPricingSimulationUseCase(
                command -> {
                    throw new PricingRuleViolationException("invalid", new IllegalArgumentException());
                },
                registry);
        PricingSimulationCommand command = new PricingSimulationCommand(
                ReceivableType.DUPLICATA_MERCANTIL,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 9, 26),
                Currency.USD);

        assertThatThrownBy(() -> metered.simulate(command))
                .isInstanceOf(PricingRuleViolationException.class);
        assertThat(registry.get("pricing.simulation.duration")
                .tags("currency", "USD", "result", "error")
                .timer()
                .count()).isOne();
    }
}
