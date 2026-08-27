package com.credit.engine.srm.currency.internal.application;

import com.credit.engine.srm.currency.FindCurrentExchangeRateUseCase;
import com.credit.engine.srm.currency.RefreshExchangeRateUseCase;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
public class CurrencyService implements FindCurrentExchangeRateUseCase, RefreshExchangeRateUseCase {

    private final ExchangeRateRepository repository;
    private final ExchangeRateProvider provider;
    private final Duration maxAge;

    public CurrencyService(
            ExchangeRateRepository repository,
            ExchangeRateProvider provider,
            CurrencyProperties properties) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.provider = Objects.requireNonNull(provider, "provider is required");
        this.maxAge = Objects.requireNonNull(properties, "properties is required").maxAge();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExchangeRate> find(
            Currency baseCurrency,
            Currency quoteCurrency,
            Instant at) {
        Objects.requireNonNull(baseCurrency, "baseCurrency is required");
        Objects.requireNonNull(quoteCurrency, "quoteCurrency is required");
        Objects.requireNonNull(at, "at is required");
        if (baseCurrency == quoteCurrency) {
            throw new IllegalArgumentException("exchange rate currencies must be different");
        }
        return repository.findCurrent(baseCurrency, quoteCurrency, at, maxAge);
    }

    @Override
    @Transactional
    public ExchangeRate refreshUsdToBrl() {
        return repository.save(provider.fetchUsdToBrl());
    }
}
