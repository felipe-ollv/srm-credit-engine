package com.credit.engine.srm.pricing;

public interface PriceReceivableUseCase {

    PricingResult price(PriceReceivableCommand command);
}
