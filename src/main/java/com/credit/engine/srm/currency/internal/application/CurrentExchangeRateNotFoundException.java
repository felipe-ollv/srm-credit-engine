package com.credit.engine.srm.currency.internal.application;

public final class CurrentExchangeRateNotFoundException extends RuntimeException {

    public CurrentExchangeRateNotFoundException() {
        super("No current exchange rate was found for the requested currency pair");
    }
}
