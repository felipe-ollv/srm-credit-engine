package com.credit.engine.srm.pricing;

public interface SimulatePricingUseCase {

    PricingSimulationResult simulate(PricingSimulationCommand command);
}
