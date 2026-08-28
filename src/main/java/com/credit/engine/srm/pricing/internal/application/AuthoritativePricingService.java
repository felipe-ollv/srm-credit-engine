package com.credit.engine.srm.pricing.internal.application;

import com.credit.engine.srm.pricing.InterestRate;
import com.credit.engine.srm.pricing.PriceReceivableCommand;
import com.credit.engine.srm.pricing.PriceReceivableUseCase;
import com.credit.engine.srm.pricing.PricingEngine;
import com.credit.engine.srm.pricing.PricingRejectedException;
import com.credit.engine.srm.pricing.PricingRequest;
import com.credit.engine.srm.pricing.PricingResult;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

public final class AuthoritativePricingService implements PriceReceivableUseCase {

    private final PricingEngine pricingEngine;
    private final InterestRate baseRate;
    private final ZoneId businessZone;

    public AuthoritativePricingService(
            PricingEngine pricingEngine,
            InterestRate baseRate,
            ZoneId businessZone) {
        this.pricingEngine = Objects.requireNonNull(pricingEngine, "pricingEngine is required");
        this.baseRate = Objects.requireNonNull(baseRate, "baseRate is required");
        this.businessZone = Objects.requireNonNull(businessZone, "businessZone is required");
    }

    @Override
    public PricingResult price(PriceReceivableCommand command) {
        Objects.requireNonNull(command, "command is required");
        LocalDate pricingDate = LocalDate.ofInstant(command.calculatedAt(), businessZone);
        try {
            return pricingEngine.price(new PricingRequest(
                    command.receivableType(),
                    command.faceValue(),
                    pricingDate,
                    command.dueDate(),
                    baseRate,
                    command.paymentCurrency(),
                    command.exchangeRate(),
                    command.calculatedAt()));
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw new PricingRejectedException(exception.getMessage(), exception);
        }
    }
}
