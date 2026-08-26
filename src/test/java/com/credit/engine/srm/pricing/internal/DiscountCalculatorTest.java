package com.credit.engine.srm.pricing.internal;

import com.credit.engine.srm.pricing.InterestRate;
import com.credit.engine.srm.pricing.Term;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountCalculatorTest {

    private final DiscountCalculator calculator = new DiscountCalculator();

    @Test
    void shouldCalculatePresentValueAndDiscountWithDecimal128AndHalfEven() {
        DiscountCalculation result = calculator.calculate(
                Money.of("100000.00", Currency.BRL),
                InterestRate.of("0.01"),
                InterestRate.of("0.015"),
                new Term(3));

        assertThat(result.presentValueBrl()).isEqualTo(Money.of("92859.94", Currency.BRL));
        assertThat(result.discountBrl()).isEqualTo(Money.of("7140.06", Currency.BRL));
    }

    @Test
    void shouldRejectNonBrlOrNonPositiveFaceValue() {
        assertThatThrownBy(() -> calculator.calculate(
                Money.of("100.00", Currency.USD),
                InterestRate.of("0.01"),
                InterestRate.of("0.015"),
                new Term(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(
                Money.of("0.00", Currency.BRL),
                InterestRate.of("0.01"),
                InterestRate.of("0.015"),
                new Term(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAllowPositiveMathematicalValueThatRoundsToZeroCents() {
        DiscountCalculation result = calculator.calculate(
                Money.of("0.01", Currency.BRL),
                InterestRate.of("1"),
                InterestRate.of("0"),
                new Term(360));

        assertThat(result.presentValueBrl()).isEqualTo(Money.of("0.00", Currency.BRL));
        assertThat(result.discountBrl()).isEqualTo(Money.of("0.01", Currency.BRL));
    }
}
