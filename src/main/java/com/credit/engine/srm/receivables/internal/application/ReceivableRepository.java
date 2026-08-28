package com.credit.engine.srm.receivables.internal.application;

import com.credit.engine.srm.receivables.ReceivableStatusView;
import com.credit.engine.srm.receivables.ReceivableView;
import com.credit.engine.srm.receivables.internal.Receivable;
import com.credit.engine.srm.shared.AssignorId;
import com.credit.engine.srm.shared.PageResult;
import com.credit.engine.srm.shared.ReceivableId;
import com.credit.engine.srm.shared.SettlementId;

import java.time.Instant;
import java.util.Optional;

public interface ReceivableRepository {

    ReceivableView save(Receivable receivable);

    Optional<Receivable> findById(ReceivableId receivableId);

    void markSettled(Receivable receivable, SettlementId settlementId, Instant settledAt);

    PageResult<ReceivableView> search(
            AssignorId assignorId,
            ReceivableStatusView status,
            int page,
            int size);
}
