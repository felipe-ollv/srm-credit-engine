package com.credit.engine.srm.receivables;

import com.credit.engine.srm.shared.AssignorId;
import com.credit.engine.srm.shared.ReceivableType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record CreateReceivableCommand(
        AssignorId assignorId,
        ReceivableType type,
        BigDecimal faceValue,
        LocalDate dueDate) {

    public CreateReceivableCommand {
        Objects.requireNonNull(assignorId, "assignorId is required");
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(faceValue, "faceValue is required");
        Objects.requireNonNull(dueDate, "dueDate is required");
    }
}
