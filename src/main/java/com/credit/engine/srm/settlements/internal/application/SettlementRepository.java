package com.credit.engine.srm.settlements.internal.application;

import com.credit.engine.srm.settlements.internal.Settlement;

import java.util.UUID;

public interface SettlementRepository {

    void save(UUID batchId, int itemIndex, Settlement settlement);
}
