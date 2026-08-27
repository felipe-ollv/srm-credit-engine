package com.credit.engine.srm.currency.internal.application;

public final class ExchangeRateProviderUnavailableException extends RuntimeException {

    public ExchangeRateProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExchangeRateProviderUnavailableException(String message) {
        super(message);
    }
}
