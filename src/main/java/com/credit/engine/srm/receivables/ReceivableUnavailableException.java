package com.credit.engine.srm.receivables;

public final class ReceivableUnavailableException extends RuntimeException {

    public ReceivableUnavailableException() {
        super("Receivable is not available for settlement");
    }

    public ReceivableUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
