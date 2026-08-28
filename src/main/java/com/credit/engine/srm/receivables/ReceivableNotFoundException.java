package com.credit.engine.srm.receivables;

public final class ReceivableNotFoundException extends RuntimeException {

    public ReceivableNotFoundException() {
        super("Receivable was not found");
    }
}
