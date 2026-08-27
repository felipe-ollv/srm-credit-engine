package com.credit.engine.srm.pricing.internal.adapter.out.fx;

import com.credit.engine.srm.currency.FindCurrentExchangeRateUseCase;
import com.credit.engine.srm.pricing.internal.application.CurrentExchangeRatePort;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class PersistedExchangeRateAdapter implements CurrentExchangeRatePort {

    private final FindCurrentExchangeRateUseCase exchangeRates;

    public PersistedExchangeRateAdapter(FindCurrentExchangeRateUseCase exchangeRates) {
        this.exchangeRates = Objects.requireNonNull(exchangeRates, "exchangeRates is required");
    }

    @Override
    public Optional<ExchangeRate> findValidUsdToBrl(Instant at) {
        return exchangeRates.find(Currency.USD, Currency.BRL, at);
    }
}
