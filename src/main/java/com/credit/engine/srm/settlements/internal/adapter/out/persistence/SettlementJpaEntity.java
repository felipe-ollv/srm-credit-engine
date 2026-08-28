package com.credit.engine.srm.settlements.internal.adapter.out.persistence;

import com.credit.engine.srm.pricing.PricingResult;
import com.credit.engine.srm.settlements.internal.Settlement;
import com.credit.engine.srm.shared.ExchangeRate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "settlements")
class SettlementJpaEntity {

    @Id UUID id;
    @Column(name = "batch_id", nullable = false) UUID batchId;
    @Column(name = "item_index", nullable = false) int itemIndex;
    @Column(name = "receivable_id", nullable = false, unique = true) UUID receivableId;
    @Column(name = "assignor_id", nullable = false) UUID assignorId;
    @Column(name = "assignor_document", nullable = false, length = 14) String assignorDocument;
    @Column(name = "assignor_legal_name", nullable = false, length = 160) String assignorLegalName;
    @Column(name = "receivable_type", nullable = false, length = 40) String receivableType;
    @Column(name = "due_date", nullable = false) LocalDate dueDate;
    @Column(name = "face_value", nullable = false, precision = 19, scale = 2) BigDecimal faceValue;
    @Column(name = "present_value", nullable = false, precision = 19, scale = 2) BigDecimal presentValue;
    @Column(nullable = false, precision = 19, scale = 2) BigDecimal discount;
    @Column(name = "payment_amount", nullable = false, precision = 19, scale = 2) BigDecimal paymentAmount;
    @Column(name = "payment_currency", nullable = false, length = 3) String paymentCurrency;
    @Column(name = "term_months", nullable = false) int termMonths;
    @Column(name = "base_rate", nullable = false, precision = 19, scale = 10) BigDecimal baseRate;
    @Column(nullable = false, precision = 19, scale = 10) BigDecimal spread;
    @Column(name = "exchange_base_currency", length = 3) String exchangeBaseCurrency;
    @Column(name = "exchange_quote_currency", length = 3) String exchangeQuoteCurrency;
    @Column(name = "exchange_rate", precision = 19, scale = 10) BigDecimal exchangeRate;
    @Column(name = "exchange_effective_at") Instant exchangeEffectiveAt;
    @Column(name = "exchange_captured_at") Instant exchangeCapturedAt;
    @Column(name = "pricing_date", nullable = false) LocalDate pricingDate;
    @Column(name = "calculated_at", nullable = false) Instant calculatedAt;
    @Column(name = "settled_at", nullable = false) Instant settledAt;

    protected SettlementJpaEntity() {
    }

    SettlementJpaEntity(UUID batchId, int itemIndex, Settlement settlement) {
        PricingResult pricing = settlement.pricingResult();
        this.id = settlement.id().value();
        this.batchId = batchId;
        this.itemIndex = itemIndex;
        this.receivableId = settlement.receivableId().value();
        this.assignorId = settlement.assignorId().value();
        this.assignorDocument = settlement.assignorDocument();
        this.assignorLegalName = settlement.assignorLegalName();
        this.receivableType = settlement.receivableType().name();
        this.dueDate = settlement.dueDate();
        this.faceValue = pricing.faceValueBrl().amount();
        this.presentValue = pricing.presentValueBrl().amount();
        this.discount = pricing.discountBrl().amount();
        this.paymentAmount = pricing.paymentAmount().amount();
        this.paymentCurrency = pricing.paymentAmount().currency().name();
        this.termMonths = pricing.term().months();
        this.baseRate = pricing.baseRate().monthlyRate();
        this.spread = pricing.spread().monthlyRate();
        pricing.exchangeRate().ifPresent(this::applyExchangeRate);
        this.pricingDate = pricing.pricingDate();
        this.calculatedAt = pricing.calculatedAt();
        this.settledAt = settlement.settledAt();
    }

    private void applyExchangeRate(ExchangeRate rate) {
        this.exchangeBaseCurrency = rate.baseCurrency().name();
        this.exchangeQuoteCurrency = rate.quoteCurrency().name();
        this.exchangeRate = rate.rate();
        this.exchangeEffectiveAt = rate.effectiveAt();
        this.exchangeCapturedAt = rate.capturedAt();
    }
}
