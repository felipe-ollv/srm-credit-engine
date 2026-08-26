package com.credit.engine.srm.pricing.internal;

import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.Money;

import java.util.Objects;

public record DiscountCalculation(Money presentValueBrl, Money discountBrl) {

    public DiscountCalculation {
        Objects.requireNonNull(presentValueBrl, "presentValueBrl is required");
        Objects.requireNonNull(discountBrl, "discountBrl is required");
        if (presentValueBrl.currency() != Currency.BRL || discountBrl.currency() != Currency.BRL) {
            throw new IllegalArgumentException("discount calculation values must be denominated in BRL");
        }
        if (presentValueBrl.isNegative()) {
            throw new IllegalArgumentException("present value cannot be negative");
        }
        if (discountBrl.isNegative()) {
            throw new IllegalArgumentException("discount cannot be negative");
        }
    }
}
