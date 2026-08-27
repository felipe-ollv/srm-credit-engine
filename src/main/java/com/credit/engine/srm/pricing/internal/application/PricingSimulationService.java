package com.credit.engine.srm.pricing.internal.application;

import com.credit.engine.srm.pricing.InterestRate;
import com.credit.engine.srm.pricing.PricingEngine;
import com.credit.engine.srm.pricing.PricingRequest;
import com.credit.engine.srm.pricing.PricingResult;
import com.credit.engine.srm.pricing.PricingSimulationCommand;
import com.credit.engine.srm.pricing.PricingSimulationResult;
import com.credit.engine.srm.pricing.SimulatePricingUseCase;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import com.credit.engine.srm.shared.Money;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

public final class PricingSimulationService implements SimulatePricingUseCase {

    private final PricingEngine pricingEngine;
    private final CurrentExchangeRatePort exchangeRatePort;
    private final InterestRate baseRate;
    private final Clock clock;
    private final ZoneId businessZone;

    public PricingSimulationService(
            PricingEngine pricingEngine,
            CurrentExchangeRatePort exchangeRatePort,
            InterestRate baseRate,
            Clock clock,
            ZoneId businessZone) {

        this.pricingEngine = Objects.requireNonNull(pricingEngine, "pricingEngine is required");
        this.exchangeRatePort = Objects.requireNonNull(exchangeRatePort, "exchangeRatePort is required");
        this.baseRate = Objects.requireNonNull(baseRate, "baseRate is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.businessZone = Objects.requireNonNull(businessZone, "businessZone is required");
    }

    @Override
    public PricingSimulationResult simulate(PricingSimulationCommand command) {
        Objects.requireNonNull(command, "command is required");
        Instant calculatedAt = clock.instant();
        LocalDate pricingDate = LocalDate.ofInstant(calculatedAt, businessZone);
        Optional<ExchangeRate> exchangeRate = resolveExchangeRate(command.paymentCurrency(), calculatedAt);

        try {
            PricingResult result = pricingEngine.price(new PricingRequest(
                    command.receivableType(),
                    new Money(command.faceValue(), Currency.BRL),
                    pricingDate,
                    command.dueDate(),
                    baseRate,
                    command.paymentCurrency(),
                    exchangeRate,
                    calculatedAt));
            return PricingSimulationResult.from(result);
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw new PricingRuleViolationException(exception.getMessage(), exception);
        }
    }

    private Optional<ExchangeRate> resolveExchangeRate(Currency paymentCurrency, Instant at) {
        if (paymentCurrency == Currency.BRL) {
            return Optional.empty();
        }
        return Optional.of(exchangeRatePort.findValidUsdToBrl(at)
                .orElseThrow(FxRateUnavailableException::new));
    }
}
