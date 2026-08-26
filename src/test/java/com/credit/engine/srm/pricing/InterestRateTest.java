package com.credit.engine.srm.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterestRateTest {

    @Test
    void shouldRepresentAndAddMonthlyDecimalRates() {
        InterestRate baseRate = InterestRate.of("0.01");
        InterestRate spread = InterestRate.of("0.015");

        assertThat(baseRate.add(spread).monthlyRate())
                .isEqualByComparingTo(new BigDecimal("0.025"));
    }

    @Test
    void shouldRejectNegativeRate() {
        assertThatThrownBy(() -> InterestRate.of("-0.0001"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
