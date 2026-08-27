package com.credit.engine.srm.receivables.internal.application;

public final class AssignorDocumentAlreadyExistsException extends RuntimeException {

    public AssignorDocumentAlreadyExistsException() {
        super("An assignor with this document already exists");
    }
}
