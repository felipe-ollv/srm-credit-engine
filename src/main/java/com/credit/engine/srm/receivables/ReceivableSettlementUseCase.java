package com.credit.engine.srm.receivables;

import com.credit.engine.srm.shared.ReceivableId;
import com.credit.engine.srm.shared.SettlementId;

import java.time.Instant;

public interface ReceivableSettlementUseCase {

    ReceivableForSettlement findAvailable(ReceivableId receivableId);

    void markSettled(ReceivableId receivableId, SettlementId settlementId, Instant settledAt);
}
