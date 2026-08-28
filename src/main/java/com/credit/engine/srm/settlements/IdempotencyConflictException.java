package com.credit.engine.srm.settlements;

public final class IdempotencyConflictException extends RuntimeException {

    private final String code;

    public IdempotencyConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
