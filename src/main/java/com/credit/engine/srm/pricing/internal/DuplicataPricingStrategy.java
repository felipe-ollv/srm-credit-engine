package com.credit.engine.srm.pricing.internal;

import com.credit.engine.srm.pricing.InterestRate;
import com.credit.engine.srm.pricing.Term;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableType;

import java.util.Objects;

public final class DuplicataPricingStrategy implements PricingStrategy {

    private static final InterestRate SPREAD = InterestRate.of("0.015");

    private final DiscountCalculator calculator;

    public DuplicataPricingStrategy(DiscountCalculator calculator) {
        this.calculator = Objects.requireNonNull(calculator, "calculator is required");
    }

    @Override
    public ReceivableType supportedType() {
        return ReceivableType.DUPLICATA_MERCANTIL;
    }

    @Override
    public InterestRate spread() {
        return SPREAD;
    }

    @Override
    public DiscountCalculation calculate(Money faceValueBrl, InterestRate baseRate, Term term) {
        return calculator.calculate(faceValueBrl, baseRate, SPREAD, term);
    }
}
