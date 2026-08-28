package com.credit.engine.srm.settlements.internal.application;

import com.credit.engine.srm.settlements.SettlementBatchResult;

public interface IdempotencyResponseCodec {

    String encode(SettlementBatchResult result);

    SettlementBatchResult decode(String payload);
}
