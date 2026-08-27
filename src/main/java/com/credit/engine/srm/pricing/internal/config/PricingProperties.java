package com.credit.engine.srm.pricing.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.ZoneId;

@ConfigurationProperties("app.pricing")
public record PricingProperties(
        BigDecimal baseRate,
        ZoneId businessZone) {

    private static final BigDecimal DEFAULT_BASE_RATE = new BigDecimal("0.01");
    private static final ZoneId DEFAULT_BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    public PricingProperties {
        baseRate = baseRate == null ? DEFAULT_BASE_RATE : baseRate;
        businessZone = businessZone == null ? DEFAULT_BUSINESS_ZONE : businessZone;

        if (baseRate.signum() < 0) {
            throw new IllegalArgumentException("app.pricing.base-rate cannot be negative");
        }
    }
}
