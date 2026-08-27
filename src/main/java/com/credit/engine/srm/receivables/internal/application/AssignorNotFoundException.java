package com.credit.engine.srm.receivables.internal.application;

public final class AssignorNotFoundException extends RuntimeException {

    public AssignorNotFoundException() {
        super("Assignor was not found");
    }
}
