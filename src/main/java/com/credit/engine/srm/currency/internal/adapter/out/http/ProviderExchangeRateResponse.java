package com.credit.engine.srm.currency.internal.adapter.out.http;

import com.credit.engine.srm.config.web.StrictStringDeserializer;
import com.credit.engine.srm.shared.Currency;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;

record ProviderExchangeRateResponse(
        Currency baseCurrency,
        Currency quoteCurrency,
        @JsonDeserialize(using = StrictStringDeserializer.class) String rate,
        Instant effectiveAt) {
}
