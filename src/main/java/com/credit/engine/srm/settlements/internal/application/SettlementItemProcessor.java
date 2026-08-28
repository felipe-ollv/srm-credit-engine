package com.credit.engine.srm.settlements.internal.application;

import com.credit.engine.srm.pricing.PricingRejectedException;
import com.credit.engine.srm.receivables.ReceivableNotFoundException;
import com.credit.engine.srm.receivables.ReceivableUnavailableException;
import com.credit.engine.srm.settlements.SettlementBatchCommand;
import com.credit.engine.srm.settlements.SettlementBatchItemResult;
import com.credit.engine.srm.settlements.SettlementItemStatus;
import com.credit.engine.srm.shared.ExchangeRate;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.UnexpectedRollbackException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SettlementItemProcessor {

    private final SettlementItemTransaction transaction;

    SettlementItemProcessor(SettlementItemTransaction transaction) {
        this.transaction = transaction;
    }

    public SettlementBatchItemResult process(
            UUID batchId,
            int itemIndex,
            SettlementBatchCommand.Item item,
            Instant processingAt,
            Optional<ExchangeRate> exchangeRate) {
        try {
            return transaction.execute(batchId, itemIndex, item, processingAt, exchangeRate);
        } catch (ReceivableNotFoundException exception) {
            return failure(item, SettlementItemStatus.NOT_FOUND, "RECEIVABLE_NOT_FOUND", exception);
        } catch (ReceivableUnavailableException
                 | OptimisticLockingFailureException
                 | DataIntegrityViolationException exception) {
            return failure(item, SettlementItemStatus.CONFLICT, "RECEIVABLE_CONFLICT", exception);
        } catch (MissingBatchExchangeRateException exception) {
            return failure(item, SettlementItemStatus.FX_RATE_UNAVAILABLE, "FX_RATE_UNAVAILABLE", exception);
        } catch (PricingRejectedException exception) {
            return failure(item, SettlementItemStatus.RULE_VIOLATION, "PRICING_RULE_VIOLATION", exception);
        } catch (DataAccessException | UnexpectedRollbackException exception) {
            return failure(item, SettlementItemStatus.TECHNICAL_ERROR, "SETTLEMENT_PERSISTENCE_FAILED", exception);
        }
    }

    private static SettlementBatchItemResult failure(
            SettlementBatchCommand.Item item,
            SettlementItemStatus status,
            String code,
            RuntimeException exception) {
        return SettlementBatchItemResult.failure(
                item.receivableId().value(),
                item.paymentCurrency().name(),
                status,
                code,
                exception.getMessage());
    }
}
