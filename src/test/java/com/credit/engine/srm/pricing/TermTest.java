package com.credit.engine.srm.pricing;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TermTest {

    private static final LocalDate PRICING_DATE = LocalDate.of(2026, 1, 15);

    @Test
    void shouldCountExactAndStartedCalendarMonths() {
        assertThat(Term.between(PRICING_DATE, LocalDate.of(2026, 4, 15)).months()).isEqualTo(3);
        assertThat(Term.between(PRICING_DATE, LocalDate.of(2026, 4, 16)).months()).isEqualTo(4);
        assertThat(Term.between(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28)).months())
                .isEqualTo(1);
    }

    @Test
    void shouldRejectNonFutureDueDate() {
        assertThatThrownBy(() -> Term.between(PRICING_DATE, PRICING_DATE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Term.between(PRICING_DATE, PRICING_DATE.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldEnforceMaximumTerm() {
        assertThat(Term.between(PRICING_DATE, PRICING_DATE.plusMonths(360)).months())
                .isEqualTo(360);
        assertThatThrownBy(() -> Term.between(
                PRICING_DATE, PRICING_DATE.plusMonths(360).plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("360");
    }
}
