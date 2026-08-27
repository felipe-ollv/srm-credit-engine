package com.credit.engine.srm.receivables;

import java.util.Objects;

public record CreateAssignorCommand(String document, String legalName) {

    public CreateAssignorCommand {
        Objects.requireNonNull(document, "document is required");
        Objects.requireNonNull(legalName, "legalName is required");
    }
}
