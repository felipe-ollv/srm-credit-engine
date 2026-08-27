package com.credit.engine.srm.pricing.internal.application;

import com.credit.engine.srm.shared.ExchangeRate;

import java.time.Instant;
import java.util.Optional;

public interface CurrentExchangeRatePort {

    Optional<ExchangeRate> findValidUsdToBrl(Instant at);
}
