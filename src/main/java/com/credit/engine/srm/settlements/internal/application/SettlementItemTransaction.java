package com.credit.engine.srm.settlements.internal.application;

import com.credit.engine.srm.pricing.PriceReceivableCommand;
import com.credit.engine.srm.pricing.PriceReceivableUseCase;
import com.credit.engine.srm.pricing.PricingResult;
import com.credit.engine.srm.receivables.ReceivableForSettlement;
import com.credit.engine.srm.receivables.ReceivableSettlementUseCase;
import com.credit.engine.srm.settlements.SettlementBatchCommand;
import com.credit.engine.srm.settlements.SettlementBatchItemResult;
import com.credit.engine.srm.settlements.SettlementResult;
import com.credit.engine.srm.settlements.internal.Settlement;
import com.credit.engine.srm.shared.ExchangeRate;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.SettlementId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SettlementItemTransaction {

    private final ReceivableSettlementUseCase receivables;
    private final PriceReceivableUseCase pricing;
    private final SettlementRepository settlements;

    SettlementItemTransaction(
            ReceivableSettlementUseCase receivables,
            PriceReceivableUseCase pricing,
            SettlementRepository settlements) {
        this.receivables = receivables;
        this.pricing = pricing;
        this.settlements = settlements;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SettlementBatchItemResult execute(
            UUID batchId,
            int itemIndex,
            SettlementBatchCommand.Item item,
            Instant processingAt,
            Optional<ExchangeRate> exchangeRate) {
        ReceivableForSettlement receivable = receivables.findAvailable(item.receivableId());
        if (item.paymentCurrency() == Currency.USD && exchangeRate.isEmpty()) {
            throw new MissingBatchExchangeRateException();
        }
        Optional<ExchangeRate> appliedExchangeRate = item.paymentCurrency() == Currency.USD
                ? exchangeRate
                : Optional.empty();
        PricingResult pricingResult = pricing.price(new PriceReceivableCommand(
                receivable.type(),
                receivable.faceValue(),
                receivable.dueDate(),
                item.paymentCurrency(),
                processingAt,
                appliedExchangeRate));
        SettlementId settlementId = SettlementId.newId();
        Settlement settlement = Settlement.create(
                settlementId,
                receivable.id(),
                receivable.assignorId(),
                receivable.assignorDocument(),
                receivable.assignorLegalName(),
                receivable.type(),
                receivable.dueDate(),
                pricingResult,
                processingAt);
        receivables.markSettled(receivable.id(), settlementId, processingAt);
        settlements.save(batchId, itemIndex, settlement);
        return SettlementBatchItemResult.success(
                receivable.id().value(),
                item.paymentCurrency().name(),
                toResult(settlement));
    }

    private static SettlementResult toResult(Settlement settlement) {
        PricingResult pricing = settlement.pricingResult();
        SettlementResult.ExchangeRateResult exchange = pricing.exchangeRate()
                .map(rate -> new SettlementResult.ExchangeRateResult(
                        rate.baseCurrency().name(),
                        rate.quoteCurrency().name(),
                        rate.rate().toPlainString(),
                        rate.effectiveAt(),
                        rate.capturedAt()))
                .orElse(null);
        return new SettlementResult(
                settlement.id().value(),
                settlement.receivableId().value(),
                settlement.assignorId().value(),
                settlement.assignorDocument(),
                settlement.assignorLegalName(),
                settlement.receivableType().name(),
                settlement.dueDate(),
                money(pricing.faceValueBrl()),
                money(pricing.presentValueBrl()),
                money(pricing.discountBrl()),
                money(pricing.paymentAmount()),
                pricing.term().months(),
                pricing.baseRate().monthlyRate().toPlainString(),
                pricing.spread().monthlyRate().toPlainString(),
                exchange,
                pricing.pricingDate(),
                pricing.calculatedAt(),
                settlement.settledAt());
    }

    private static SettlementResult.MoneyResult money(com.credit.engine.srm.shared.Money money) {
        return new SettlementResult.MoneyResult(
                money.amount().toPlainString(), money.currency().name());
    }
}
