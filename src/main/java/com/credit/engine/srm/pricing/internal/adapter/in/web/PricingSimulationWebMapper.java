package com.credit.engine.srm.pricing.internal.adapter.in.web;

import com.credit.engine.srm.pricing.PricingSimulationCommand;
import com.credit.engine.srm.pricing.PricingSimulationResult;
import com.credit.engine.srm.shared.ExchangeRate;
import com.credit.engine.srm.shared.Money;

import java.math.BigDecimal;

final class PricingSimulationWebMapper {

    private PricingSimulationWebMapper() {
    }

    static PricingSimulationCommand toCommand(PricingSimulationRequestDto request) {
        return new PricingSimulationCommand(
                request.receivableType(),
                new BigDecimal(request.faceValue()),
                request.dueDate(),
                request.paymentCurrency());
    }

    static PricingSimulationResponseDto toResponse(PricingSimulationResult result) {
        return new PricingSimulationResponseDto(
                result.receivableType().name(),
                money(result.faceValue()),
                money(result.presentValue()),
                money(result.discount()),
                money(result.payment()),
                result.termMonths(),
                result.baseRate().monthlyRate().toPlainString(),
                result.spread().monthlyRate().toPlainString(),
                result.exchangeRate().map(PricingSimulationWebMapper::exchangeRate).orElse(null),
                result.pricingDate(),
                result.calculatedAt());
    }

    private static MoneyResponseDto money(Money money) {
        return new MoneyResponseDto(money.amount().toPlainString(), money.currency().name());
    }

    private static ExchangeRateResponseDto exchangeRate(ExchangeRate exchangeRate) {
        return new ExchangeRateResponseDto(
                exchangeRate.baseCurrency().name(),
                exchangeRate.quoteCurrency().name(),
                exchangeRate.rate().toPlainString(),
                exchangeRate.effectiveAt(),
                exchangeRate.capturedAt());
    }
}
