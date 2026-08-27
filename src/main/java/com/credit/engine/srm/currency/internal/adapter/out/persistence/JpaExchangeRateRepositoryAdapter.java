package com.credit.engine.srm.currency.internal.adapter.out.persistence;

import com.credit.engine.srm.currency.internal.application.ExchangeRateRepository;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaExchangeRateRepositoryAdapter implements ExchangeRateRepository {

    private final SpringDataExchangeRateRepository repository;

    JpaExchangeRateRepositoryAdapter(SpringDataExchangeRateRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExchangeRate save(ExchangeRate exchangeRate) {
        ExchangeRateJpaEntity saved = repository.saveAndFlush(new ExchangeRateJpaEntity(
                UUID.randomUUID(),
                exchangeRate.baseCurrency(),
                exchangeRate.quoteCurrency(),
                exchangeRate.rate(),
                exchangeRate.effectiveAt(),
                exchangeRate.capturedAt()));
        return toDomain(saved);
    }

    @Override
    public Optional<ExchangeRate> findCurrent(
            Currency baseCurrency,
            Currency quoteCurrency,
            Instant at,
            Duration maxAge) {
        return repository
                .findFirstByBaseCurrencyAndQuoteCurrencyAndEffectiveAtLessThanEqualAndEffectiveAtGreaterThanEqualOrderByEffectiveAtDescCapturedAtDesc(
                        baseCurrency, quoteCurrency, at, at.minus(maxAge))
                .map(JpaExchangeRateRepositoryAdapter::toDomain);
    }

    private static ExchangeRate toDomain(ExchangeRateJpaEntity entity) {
        return new ExchangeRate(
                entity.baseCurrency,
                entity.quoteCurrency,
                entity.rate,
                entity.effectiveAt,
                entity.capturedAt);
    }
}
