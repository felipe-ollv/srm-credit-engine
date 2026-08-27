package com.credit.engine.srm.pricing.internal.adapter.in.web;

import com.credit.engine.srm.config.web.ApiProblems;
import com.credit.engine.srm.pricing.internal.application.FxRateUnavailableException;
import com.credit.engine.srm.pricing.internal.application.PricingRuleViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PricingSimulationController.class)
class PricingApiExceptionHandler {

    @ExceptionHandler(PricingRuleViolationException.class)
    ProblemDetail handlePricingRule(PricingRuleViolationException exception, HttpServletRequest request) {
        return ApiProblems.create(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "PRICING_RULE_VIOLATION",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(FxRateUnavailableException.class)
    ProblemDetail handleFxUnavailable(FxRateUnavailableException exception, HttpServletRequest request) {
        return ApiProblems.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "FX_RATE_UNAVAILABLE",
                exception.getMessage(),
                request);
    }

}
