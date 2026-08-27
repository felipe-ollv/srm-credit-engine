package com.credit.engine.srm.currency.internal.adapter.out.persistence;

import com.credit.engine.srm.shared.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface SpringDataExchangeRateRepository extends JpaRepository<ExchangeRateJpaEntity, UUID> {

    Optional<ExchangeRateJpaEntity>
    findFirstByBaseCurrencyAndQuoteCurrencyAndEffectiveAtLessThanEqualAndEffectiveAtGreaterThanEqualOrderByEffectiveAtDescCapturedAtDesc(
            Currency baseCurrency,
            Currency quoteCurrency,
            Instant at,
            Instant oldestEffectiveAt);
}
