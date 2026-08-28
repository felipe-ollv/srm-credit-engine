package com.credit.engine.srm.receivables;

import com.credit.engine.srm.shared.AssignorId;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableId;
import com.credit.engine.srm.shared.ReceivableType;

import java.time.LocalDate;

public record ReceivableForSettlement(
        ReceivableId id,
        AssignorId assignorId,
        String assignorDocument,
        String assignorLegalName,
        ReceivableType type,
        Money faceValue,
        LocalDate dueDate) {
}
