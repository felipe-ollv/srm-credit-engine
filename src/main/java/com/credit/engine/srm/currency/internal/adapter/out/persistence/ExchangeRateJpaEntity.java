package com.credit.engine.srm.currency.internal.adapter.out.persistence;

import com.credit.engine.srm.shared.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exchange_rates")
class ExchangeRateJpaEntity {

    @Id
    UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", nullable = false, length = 3)
    Currency baseCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_currency", nullable = false, length = 3)
    Currency quoteCurrency;

    @Column(nullable = false, precision = 19, scale = 10)
    BigDecimal rate;

    @Column(name = "effective_at", nullable = false)
    Instant effectiveAt;

    @Column(name = "captured_at", nullable = false)
    Instant capturedAt;

    protected ExchangeRateJpaEntity() {
    }

    ExchangeRateJpaEntity(
            UUID id,
            Currency baseCurrency,
            Currency quoteCurrency,
            BigDecimal rate,
            Instant effectiveAt,
            Instant capturedAt) {
        this.id = id;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.rate = rate;
        this.effectiveAt = effectiveAt;
        this.capturedAt = capturedAt;
    }
}
