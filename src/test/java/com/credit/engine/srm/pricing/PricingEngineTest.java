package com.credit.engine.srm.pricing;

import com.credit.engine.srm.pricing.internal.DiscountCalculator;
import com.credit.engine.srm.pricing.internal.DuplicataPricingStrategy;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingEngineTest {

    private static final LocalDate PRICING_DATE = LocalDate.of(2026, 1, 15);
    private static final Instant CALCULATED_AT = Instant.parse("2026-01-15T13:00:00Z");
    private static final InterestRate BASE_RATE = InterestRate.of("0.01");

    private final PricingEngine engine = PricingEngine.standard();

    @Test
    void shouldMatchGoldenCaseC1ForDuplicataInBrl() {
        PricingResult result = engine.price(request(
                ReceivableType.DUPLICATA_MERCANTIL,
                "100000.00",
                3,
                Currency.BRL,
                Optional.empty()));

        assertThat(result.presentValueBrl()).isEqualTo(Money.of("92859.94", Currency.BRL));
        assertThat(result.discountBrl()).isEqualTo(Money.of("7140.06", Currency.BRL));
        assertThat(result.paymentAmount()).isEqualTo(Money.of("92859.94", Currency.BRL));
        assertThat(result.spread()).isEqualTo(InterestRate.of("0.015"));
    }

    @Test
    void shouldMatchGoldenCaseC2ForPostDatedCheckInBrl() {
        PricingResult result = engine.price(request(
                ReceivableType.CHEQUE_PRE_DATADO,
                "25000.00",
                2,
                Currency.BRL,
                Optional.empty()));

        assertThat(result.presentValueBrl()).isEqualTo(Money.of("23337.77", Currency.BRL));
        assertThat(result.discountBrl()).isEqualTo(Money.of("1662.23", Currency.BRL));
        assertThat(result.paymentAmount()).isEqualTo(Money.of("23337.77", Currency.BRL));
        assertThat(result.spread()).isEqualTo(InterestRate.of("0.025"));
    }

    @Test
    void shouldMatchGoldenCaseC3ByConvertingRoundedBrlPresentValue() {
        ExchangeRate rate = usdToBrl("5.4321");

        PricingResult result = engine.price(request(
                ReceivableType.DUPLICATA_MERCANTIL,
                "100000.00",
                3,
                Currency.USD,
                Optional.of(rate)));

        assertThat(result.presentValueBrl()).isEqualTo(Money.of("92859.94", Currency.BRL));
        assertThat(result.paymentAmount()).isEqualTo(Money.of("17094.67", Currency.USD));
        assertThat(result.discountBrl()).isEqualTo(Money.of("7140.06", Currency.BRL));
        assertThat(result.exchangeRate()).contains(rate);
    }

    @Test
    void shouldRejectMissingUnexpectedOrWrongDirectionExchangeRate() {
        assertThatThrownBy(() -> engine.price(request(
                ReceivableType.DUPLICATA_MERCANTIL,
                "100000.00",
                3,
                Currency.USD,
                Optional.empty())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires an exchange rate");

        assertThatThrownBy(() -> engine.price(request(
                ReceivableType.DUPLICATA_MERCANTIL,
                "100000.00",
                3,
                Currency.BRL,
                Optional.of(usdToBrl("5.4321")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot have");

        ExchangeRate inversePair = new ExchangeRate(
                Currency.BRL,
                Currency.USD,
                new BigDecimal("0.1841"),
                CALCULATED_AT.minusSeconds(60),
                CALCULATED_AT);
        assertThatThrownBy(() -> engine.price(request(
                ReceivableType.DUPLICATA_MERCANTIL,
                "100000.00",
                3,
                Currency.USD,
                Optional.of(inversePair))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("USD/BRL");
    }

    @Test
    void shouldRejectDuplicateAndMissingStrategyRegistration() {
        DiscountCalculator calculator = new DiscountCalculator();

        assertThatThrownBy(() -> new PricingEngine(List.of(
                new DuplicataPricingStrategy(calculator),
                new DuplicataPricingStrategy(calculator))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");

        PricingEngine incompleteEngine = new PricingEngine(
                List.of(new DuplicataPricingStrategy(calculator)));
        assertThatThrownBy(() -> incompleteEngine.price(request(
                ReceivableType.CHEQUE_PRE_DATADO,
                "25000.00",
                2,
                Currency.BRL,
                Optional.empty())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no pricing strategy");
    }

    @Test
    void shouldRejectInconsistentPricingResult() {
        assertThatThrownBy(() -> new PricingResult(
                ReceivableType.DUPLICATA_MERCANTIL,
                Money.of("100.00", Currency.BRL),
                Money.of("90.00", Currency.BRL),
                Money.of("9.00", Currency.BRL),
                Money.of("90.00", Currency.BRL),
                new Term(1),
                BASE_RATE,
                InterestRate.of("0.015"),
                Optional.empty(),
                PRICING_DATE,
                CALCULATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("discount");
    }

    private static PricingRequest request(
            ReceivableType type,
            String faceValue,
            int termMonths,
            Currency paymentCurrency,
            Optional<ExchangeRate> exchangeRate) {

        return new PricingRequest(
                type,
                Money.of(faceValue, Currency.BRL),
                PRICING_DATE,
                PRICING_DATE.plusMonths(termMonths),
                BASE_RATE,
                paymentCurrency,
                exchangeRate,
                CALCULATED_AT);
    }

    private static ExchangeRate usdToBrl(String rate) {
        return new ExchangeRate(
                Currency.USD,
                Currency.BRL,
                new BigDecimal(rate),
                CALCULATED_AT.minusSeconds(60),
                CALCULATED_AT);
    }
}
