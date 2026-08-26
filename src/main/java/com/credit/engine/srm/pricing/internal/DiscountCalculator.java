package com.credit.engine.srm.pricing.internal;

import com.credit.engine.srm.pricing.InterestRate;
import com.credit.engine.srm.pricing.Term;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.Money;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

public final class DiscountCalculator {

    private static final MathContext CALCULATION_CONTEXT = MathContext.DECIMAL128;

    public DiscountCalculation calculate(
            Money faceValueBrl,
            InterestRate baseRate,
            InterestRate spread,
            Term term) {

        Objects.requireNonNull(faceValueBrl, "faceValueBrl is required");
        Objects.requireNonNull(baseRate, "baseRate is required");
        Objects.requireNonNull(spread, "spread is required");
        Objects.requireNonNull(term, "term is required");

        if (faceValueBrl.currency() != Currency.BRL) {
            throw new IllegalArgumentException("faceValue must be denominated in BRL");
        }
        if (!faceValueBrl.isPositive()) {
            throw new IllegalArgumentException("faceValue must be positive");
        }

        BigDecimal periodicFactor = BigDecimal.ONE
                .add(baseRate.monthlyRate(), CALCULATION_CONTEXT)
                .add(spread.monthlyRate(), CALCULATION_CONTEXT);
        BigDecimal accumulatedFactor = periodicFactor.pow(term.months(), CALCULATION_CONTEXT);
        BigDecimal rawPresentValue = faceValueBrl.amount()
                .divide(accumulatedFactor, CALCULATION_CONTEXT);

        Money presentValue = Money.rounded(rawPresentValue, Currency.BRL);
        Money discount = faceValueBrl.subtract(presentValue);
        return new DiscountCalculation(presentValue, discount);
    }
}
