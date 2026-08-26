package com.credit.engine.srm.pricing;

import com.credit.engine.srm.pricing.internal.DiscountCalculation;
import com.credit.engine.srm.pricing.internal.DiscountCalculator;
import com.credit.engine.srm.pricing.internal.DuplicataPricingStrategy;
import com.credit.engine.srm.pricing.internal.PostDatedCheckPricingStrategy;
import com.credit.engine.srm.pricing.internal.PricingStrategy;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableType;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class PricingEngine {

    private final Map<ReceivableType, PricingStrategy> strategies;

    PricingEngine(Collection<? extends PricingStrategy> strategies) {
        Objects.requireNonNull(strategies, "strategies are required");
        EnumMap<ReceivableType, PricingStrategy> byType = new EnumMap<>(ReceivableType.class);

        for (PricingStrategy strategy : strategies) {
            Objects.requireNonNull(strategy, "strategy is required");
            PricingStrategy previous = byType.putIfAbsent(strategy.supportedType(), strategy);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "duplicate pricing strategy for " + strategy.supportedType());
            }
        }

        this.strategies = Map.copyOf(byType);
    }

    public static PricingEngine standard() {
        DiscountCalculator calculator = new DiscountCalculator();
        return new PricingEngine(java.util.List.of(
                new DuplicataPricingStrategy(calculator),
                new PostDatedCheckPricingStrategy(calculator)));
    }

    public PricingResult price(PricingRequest request) {
        Objects.requireNonNull(request, "request is required");
        Term term = Term.between(request.pricingDate(), request.dueDate());
        PricingStrategy strategy = strategyFor(request.receivableType());
        ExchangeRate exchangeRate = validateExchangeRate(request);

        DiscountCalculation calculation = strategy.calculate(
                request.faceValue(), request.baseRate(), term);

        Money paymentAmount = request.paymentCurrency() == Currency.BRL
                ? calculation.presentValueBrl()
                : exchangeRate.convert(calculation.presentValueBrl(), Currency.USD);

        return new PricingResult(
                request.receivableType(),
                request.faceValue(),
                calculation.presentValueBrl(),
                calculation.discountBrl(),
                paymentAmount,
                term,
                request.baseRate(),
                strategy.spread(),
                request.exchangeRate(),
                request.pricingDate(),
                request.calculatedAt());
    }

    private PricingStrategy strategyFor(ReceivableType receivableType) {
        PricingStrategy strategy = strategies.get(receivableType);
        if (strategy == null) {
            throw new IllegalStateException("no pricing strategy registered for " + receivableType);
        }
        return strategy;
    }

    private ExchangeRate validateExchangeRate(PricingRequest request) {
        if (request.paymentCurrency() == Currency.BRL) {
            if (request.exchangeRate().isPresent()) {
                throw new IllegalArgumentException("BRL payment cannot have an exchange rate");
            }
            return null;
        }

        ExchangeRate exchangeRate = request.exchangeRate().orElseThrow(
                () -> new IllegalArgumentException("USD payment requires an exchange rate"));
        if (!exchangeRate.isUsdToBrl()) {
            throw new IllegalArgumentException("USD payment requires an USD/BRL exchange rate");
        }
        return exchangeRate;
    }
}
