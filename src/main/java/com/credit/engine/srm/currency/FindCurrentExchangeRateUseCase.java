package com.credit.engine.srm.currency;

import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;

import java.time.Instant;
import java.util.Optional;

public interface FindCurrentExchangeRateUseCase {

    Optional<ExchangeRate> find(Currency baseCurrency, Currency quoteCurrency, Instant at);
}
