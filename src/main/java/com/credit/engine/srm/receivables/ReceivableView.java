package com.credit.engine.srm.receivables;

import com.credit.engine.srm.shared.AssignorId;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableId;
import com.credit.engine.srm.shared.ReceivableType;

import java.time.Instant;
import java.time.LocalDate;

public record ReceivableView(
        ReceivableId id,
        AssignorId assignorId,
        ReceivableType type,
        Money faceValue,
        LocalDate dueDate,
        LocalDate registrationDate,
        ReceivableStatusView status,
        Instant createdAt) {
}
