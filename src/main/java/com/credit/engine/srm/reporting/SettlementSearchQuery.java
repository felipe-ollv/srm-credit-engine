package com.credit.engine.srm.reporting;

import com.credit.engine.srm.shared.Currency;

import java.time.LocalDate;
import java.util.UUID;

public record SettlementSearchQuery(
        LocalDate from,
        LocalDate to,
        UUID assignorId,
        Currency paymentCurrency,
        int page,
        int size,
        SettlementSort sort) {
}
