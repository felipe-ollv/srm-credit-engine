package com.credit.engine.srm.pricing.internal.adapter.in.web;

import com.credit.engine.srm.pricing.InterestRate;
import com.credit.engine.srm.pricing.PricingSimulationCommand;
import com.credit.engine.srm.pricing.PricingSimulationResult;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PricingSimulationWebMapperTest {

    @Test
    void shouldMapTransportTypesWithoutSerializingDomainObjects() {
        LocalDate pricingDate = LocalDate.of(2026, 8, 26);
        PricingSimulationRequestDto request = new PricingSimulationRequestDto(
                ReceivableType.DUPLICATA_MERCANTIL,
                "100000.00",
                pricingDate.plusMonths(3),
                Currency.BRL);

        PricingSimulationCommand command = PricingSimulationWebMapper.toCommand(request);

        assertThat(command.faceValue()).isEqualByComparingTo("100000.00");
        assertThat(command.paymentCurrency()).isEqualTo(Currency.BRL);

        PricingSimulationResponseDto response = PricingSimulationWebMapper.toResponse(
                new PricingSimulationResult(
                        ReceivableType.DUPLICATA_MERCANTIL,
                        Money.of("100000.00", Currency.BRL),
                        Money.of("92859.94", Currency.BRL),
                        Money.of("7140.06", Currency.BRL),
                        Money.of("92859.94", Currency.BRL),
                        3,
                        InterestRate.of("0.01"),
                        InterestRate.of("0.015"),
                        Optional.empty(),
                        pricingDate,
                        Instant.parse("2026-08-26T19:00:00Z")));

        assertThat(response.presentValue().amount()).isEqualTo("92859.94");
        assertThat(response.presentValue().currency()).isEqualTo("BRL");
        assertThat(response.exchangeRate()).isNull();
    }
}
