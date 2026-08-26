package com.credit.engine.srm.pricing.internal;

import com.credit.engine.srm.pricing.InterestRate;
import com.credit.engine.srm.pricing.Term;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PricingStrategyTest {

    private final DiscountCalculator calculator = new DiscountCalculator();

    @Test
    void shouldApplyDuplicataPolicyThroughSharedCalculator() {
        PricingStrategy strategy = new DuplicataPricingStrategy(calculator);

        DiscountCalculation result = strategy.calculate(
                Money.of("100000.00", Currency.BRL),
                InterestRate.of("0.01"),
                new Term(3));

        assertThat(strategy.supportedType()).isEqualTo(ReceivableType.DUPLICATA_MERCANTIL);
        assertThat(strategy.spread()).isEqualTo(InterestRate.of("0.015"));
        assertThat(result.presentValueBrl()).isEqualTo(Money.of("92859.94", Currency.BRL));
    }

    @Test
    void shouldApplyPostDatedCheckPolicyThroughSharedCalculator() {
        PricingStrategy strategy = new PostDatedCheckPricingStrategy(calculator);

        DiscountCalculation result = strategy.calculate(
                Money.of("25000.00", Currency.BRL),
                InterestRate.of("0.01"),
                new Term(2));

        assertThat(strategy.supportedType()).isEqualTo(ReceivableType.CHEQUE_PRE_DATADO);
        assertThat(strategy.spread()).isEqualTo(InterestRate.of("0.025"));
        assertThat(result.presentValueBrl()).isEqualTo(Money.of("23337.77", Currency.BRL));
    }
}
