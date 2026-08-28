package com.credit.engine.srm.pricing;

public final class PricingRejectedException extends RuntimeException {

    public PricingRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
