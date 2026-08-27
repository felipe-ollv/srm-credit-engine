package com.credit.engine.srm.currency.internal.application;

import com.credit.engine.srm.shared.ExchangeRate;

public interface ExchangeRateProvider {

    ExchangeRate fetchUsdToBrl();
}
