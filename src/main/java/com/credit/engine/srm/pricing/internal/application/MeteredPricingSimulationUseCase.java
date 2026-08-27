package com.credit.engine.srm.pricing.internal.application;

import com.credit.engine.srm.pricing.PricingSimulationCommand;
import com.credit.engine.srm.pricing.PricingSimulationResult;
import com.credit.engine.srm.pricing.SimulatePricingUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Objects;

public final class MeteredPricingSimulationUseCase implements SimulatePricingUseCase {

    private static final String METRIC_NAME = "pricing.simulation.duration";

    private final SimulatePricingUseCase delegate;
    private final MeterRegistry meterRegistry;

    public MeteredPricingSimulationUseCase(
            SimulatePricingUseCase delegate,
            MeterRegistry meterRegistry) {

        this.delegate = Objects.requireNonNull(delegate, "delegate is required");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry is required");
    }

    @Override
    public PricingSimulationResult simulate(PricingSimulationCommand command) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            return delegate.simulate(command);
        } catch (RuntimeException exception) {
            result = "error";
            throw exception;
        } finally {
            sample.stop(Timer.builder(METRIC_NAME)
                    .description("Pricing simulation latency")
                    .tag("currency", command.paymentCurrency().name())
                    .tag("result", result)
                    .register(meterRegistry));
        }
    }
}
