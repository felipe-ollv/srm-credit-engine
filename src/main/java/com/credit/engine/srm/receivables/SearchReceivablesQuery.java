package com.credit.engine.srm.receivables;

import com.credit.engine.srm.shared.AssignorId;

public record SearchReceivablesQuery(
        AssignorId assignorId,
        ReceivableStatusView status,
        int page,
        int size) {
}
