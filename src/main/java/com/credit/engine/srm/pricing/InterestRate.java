package com.credit.engine.srm.pricing;

import java.math.BigDecimal;
import java.util.Objects;

public record InterestRate(BigDecimal monthlyRate) {

    public InterestRate {
        Objects.requireNonNull(monthlyRate, "monthlyRate is required");
        if (monthlyRate.signum() < 0) {
            throw new IllegalArgumentException("monthlyRate cannot be negative");
        }
        monthlyRate = monthlyRate.stripTrailingZeros();
    }

    public static InterestRate of(String monthlyRate) {
        return new InterestRate(new BigDecimal(monthlyRate));
    }

    public InterestRate add(InterestRate other) {
        Objects.requireNonNull(other, "interest rate is required");
        return new InterestRate(monthlyRate.add(other.monthlyRate));
    }
}
