package com.credit.engine.srm.pricing.internal.config;

import com.credit.engine.srm.currency.FindCurrentExchangeRateUseCase;
import com.credit.engine.srm.pricing.InterestRate;
import com.credit.engine.srm.pricing.PriceReceivableUseCase;
import com.credit.engine.srm.pricing.PricingEngine;
import com.credit.engine.srm.pricing.SimulatePricingUseCase;
import com.credit.engine.srm.pricing.internal.adapter.out.fx.PersistedExchangeRateAdapter;
import com.credit.engine.srm.pricing.internal.application.CurrentExchangeRatePort;
import com.credit.engine.srm.pricing.internal.application.AuthoritativePricingService;
import com.credit.engine.srm.pricing.internal.application.MeteredPricingSimulationUseCase;
import com.credit.engine.srm.pricing.internal.application.PricingSimulationService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PricingProperties.class)
public class PricingModuleConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    PricingEngine pricingEngine() {
        return PricingEngine.standard();
    }

    @Bean
    PriceReceivableUseCase priceReceivableUseCase(
            PricingEngine pricingEngine,
            PricingProperties properties) {
        return new AuthoritativePricingService(
                pricingEngine,
                new InterestRate(properties.baseRate()),
                properties.businessZone());
    }

    @Bean
    CurrentExchangeRatePort currentExchangeRatePort(FindCurrentExchangeRateUseCase exchangeRates) {
        return new PersistedExchangeRateAdapter(exchangeRates);
    }

    @Bean
    SimulatePricingUseCase simulatePricingUseCase(
            PricingEngine pricingEngine,
            CurrentExchangeRatePort exchangeRatePort,
            PricingProperties properties,
            Clock clock,
            MeterRegistry meterRegistry) {

        PricingSimulationService service = new PricingSimulationService(
                pricingEngine,
                exchangeRatePort,
                new InterestRate(properties.baseRate()),
                clock,
                properties.businessZone());
        return new MeteredPricingSimulationUseCase(service, meterRegistry);
    }
}
