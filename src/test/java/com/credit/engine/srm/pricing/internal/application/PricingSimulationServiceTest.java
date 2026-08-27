package com.credit.engine.srm.pricing.internal.application;

import com.credit.engine.srm.pricing.InterestRate;
import com.credit.engine.srm.pricing.PricingEngine;
import com.credit.engine.srm.pricing.PricingSimulationCommand;
import com.credit.engine.srm.pricing.PricingSimulationResult;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingSimulationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T19:00:00Z");
    private static final LocalDate PRICING_DATE = LocalDate.of(2026, 8, 26);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    @Test
    void shouldUseServerParametersAndSkipFxForBrl() {
        AtomicBoolean fxConsulted = new AtomicBoolean();
        CurrentExchangeRatePort port = at -> {
            fxConsulted.set(true);
            return Optional.empty();
        };
        PricingSimulationService service = service(port);

        PricingSimulationResult result = service.simulate(command(
                ReceivableType.DUPLICATA_MERCANTIL,
                "100000.00",
                PRICING_DATE.plusMonths(3),
                Currency.BRL));

        assertThat(fxConsulted).isFalse();
        assertThat(result.presentValue()).isEqualTo(Money.of("92859.94", Currency.BRL));
        assertThat(result.baseRate()).isEqualTo(InterestRate.of("0.01"));
        assertThat(result.pricingDate()).isEqualTo(PRICING_DATE);
        assertThat(result.calculatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldResolveFxForUsd() {
        ExchangeRate rate = new ExchangeRate(
                Currency.USD,
                Currency.BRL,
                new BigDecimal("5.4321"),
                NOW,
                NOW);
        PricingSimulationService service = service(at -> Optional.of(rate));

        PricingSimulationResult result = service.simulate(command(
                ReceivableType.DUPLICATA_MERCANTIL,
                "100000.00",
                PRICING_DATE.plusMonths(3),
                Currency.USD));

        assertThat(result.payment()).isEqualTo(Money.of("17094.67", Currency.USD));
        assertThat(result.exchangeRate()).contains(rate);
    }

    @Test
    void shouldReportUnavailableFxAndDomainViolations() {
        PricingSimulationService service = service(at -> Optional.empty());

        assertThatThrownBy(() -> service.simulate(command(
                ReceivableType.DUPLICATA_MERCANTIL,
                "100000.00",
                PRICING_DATE.plusMonths(3),
                Currency.USD)))
                .isInstanceOf(FxRateUnavailableException.class);

        assertThatThrownBy(() -> service.simulate(command(
                ReceivableType.DUPLICATA_MERCANTIL,
                "100000.00",
                PRICING_DATE,
                Currency.BRL)))
                .isInstanceOf(PricingRuleViolationException.class)
                .hasMessageContaining("dueDate");
    }

    private static PricingSimulationService service(CurrentExchangeRatePort port) {
        return new PricingSimulationService(
                PricingEngine.standard(),
                port,
                InterestRate.of("0.01"),
                CLOCK,
                BUSINESS_ZONE);
    }

    private static PricingSimulationCommand command(
            ReceivableType type,
            String faceValue,
            LocalDate dueDate,
            Currency paymentCurrency) {

        return new PricingSimulationCommand(
                type,
                new BigDecimal(faceValue),
                dueDate,
                paymentCurrency);
    }
}
