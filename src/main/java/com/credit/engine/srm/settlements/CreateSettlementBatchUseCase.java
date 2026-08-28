package com.credit.engine.srm.settlements;

public interface CreateSettlementBatchUseCase {

    SettlementBatchResult create(String idempotencyKey, SettlementBatchCommand command);
}
