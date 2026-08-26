package com.credit.engine.srm.pricing.internal;

import com.credit.engine.srm.pricing.InterestRate;
import com.credit.engine.srm.pricing.Term;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableType;

public interface PricingStrategy {

    ReceivableType supportedType();

    InterestRate spread();

    DiscountCalculation calculate(Money faceValueBrl, InterestRate baseRate, Term term);
}
