package com.credit.engine.srm.pricing.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.ZoneId;

@ConfigurationProperties("app.pricing")
public record PricingProperties(
        BigDecimal baseRate,
        BigDecimal usdBrlRate,
        ZoneId businessZone) {

    private static final BigDecimal DEFAULT_BASE_RATE = new BigDecimal("0.01");
    private static final BigDecimal DEFAULT_USD_BRL_RATE = new BigDecimal("5.4321");
    private static final ZoneId DEFAULT_BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    public PricingProperties {
        baseRate = baseRate == null ? DEFAULT_BASE_RATE : baseRate;
        usdBrlRate = usdBrlRate == null ? DEFAULT_USD_BRL_RATE : usdBrlRate;
        businessZone = businessZone == null ? DEFAULT_BUSINESS_ZONE : businessZone;

        if (baseRate.signum() < 0) {
            throw new IllegalArgumentException("app.pricing.base-rate cannot be negative");
        }
        if (usdBrlRate.signum() <= 0) {
            throw new IllegalArgumentException("app.pricing.usd-brl-rate must be positive");
        }
    }
}
