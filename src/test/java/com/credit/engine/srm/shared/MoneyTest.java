package com.credit.engine.srm.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void shouldCanonicalizeExactValuesAndAcceptNegativeAmounts() {
        assertThat(Money.of("10", Currency.BRL)).isEqualTo(Money.of("10.00", Currency.BRL));
        assertThat(Money.of("-1.25", Currency.USD).isNegative()).isTrue();
    }

    @Test
    void shouldRejectFractionsSmallerThanOneCentWhenCreationIsExact() {
        assertThatThrownBy(() -> Money.of("1.001", Currency.BRL))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void shouldRoundExplicitlyUsingHalfEven() {
        assertThat(Money.rounded(new BigDecimal("2.345"), Currency.BRL))
                .isEqualTo(Money.of("2.34", Currency.BRL));
        assertThat(Money.rounded(new BigDecimal("2.355"), Currency.BRL))
                .isEqualTo(Money.of("2.36", Currency.BRL));
    }

    @Test
    void shouldRejectArithmeticAcrossCurrencies() {
        Money brl = Money.of("10.00", Currency.BRL);
        Money usd = Money.of("1.00", Currency.USD);

        assertThatThrownBy(() -> brl.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currencies");
        assertThatThrownBy(() -> brl.subtract(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currencies");
    }
}
