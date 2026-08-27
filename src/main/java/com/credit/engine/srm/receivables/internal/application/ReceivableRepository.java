package com.credit.engine.srm.receivables.internal.application;

import com.credit.engine.srm.receivables.ReceivableStatusView;
import com.credit.engine.srm.receivables.ReceivableView;
import com.credit.engine.srm.receivables.internal.Receivable;
import com.credit.engine.srm.shared.AssignorId;
import com.credit.engine.srm.shared.PageResult;

public interface ReceivableRepository {

    ReceivableView save(Receivable receivable);

    PageResult<ReceivableView> search(
            AssignorId assignorId,
            ReceivableStatusView status,
            int page,
            int size);
}
