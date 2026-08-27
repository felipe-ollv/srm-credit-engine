package com.credit.engine.srm.currency.internal.application;

import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface ExchangeRateRepository {

    ExchangeRate save(ExchangeRate exchangeRate);

    Optional<ExchangeRate> findCurrent(
            Currency baseCurrency,
            Currency quoteCurrency,
            Instant at,
            Duration maxAge);
}
