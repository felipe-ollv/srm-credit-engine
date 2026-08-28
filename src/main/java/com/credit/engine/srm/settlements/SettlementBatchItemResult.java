package com.credit.engine.srm.settlements;

import java.util.UUID;

public record SettlementBatchItemResult(
        UUID receivableId,
        String paymentCurrency,
        SettlementItemStatus status,
        String code,
        String detail,
        SettlementResult settlement) {

    public static SettlementBatchItemResult success(
            UUID receivableId,
            String paymentCurrency,
            SettlementResult settlement) {
        return new SettlementBatchItemResult(
                receivableId, paymentCurrency, SettlementItemStatus.SUCCESS, null, null, settlement);
    }

    public static SettlementBatchItemResult failure(
            UUID receivableId,
            String paymentCurrency,
            SettlementItemStatus status,
            String code,
            String detail) {
        return new SettlementBatchItemResult(
                receivableId, paymentCurrency, status, code, detail, null);
    }
}
