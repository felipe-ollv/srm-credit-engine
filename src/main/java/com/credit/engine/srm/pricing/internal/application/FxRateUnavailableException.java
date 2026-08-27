package com.credit.engine.srm.pricing.internal.application;

public final class FxRateUnavailableException extends RuntimeException {

    public FxRateUnavailableException() {
        super("No valid USD/BRL exchange rate is available");
    }
}
