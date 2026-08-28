package com.credit.engine.srm.reporting.internal.application;

import com.credit.engine.srm.reporting.SettlementSort;
import com.credit.engine.srm.shared.Currency;

import java.time.Instant;
import java.util.UUID;

public record SettlementSearchCriteria(
        Instant fromInclusive,
        Instant toExclusive,
        UUID assignorId,
        Currency paymentCurrency,
        int page,
        int size,
        SettlementSort sort) {
}
