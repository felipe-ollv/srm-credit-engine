package com.credit.engine.srm.pricing.internal.adapter.in.web;

import com.credit.engine.srm.pricing.internal.application.FxRateUnavailableException;
import com.credit.engine.srm.pricing.internal.application.PricingRuleViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = PricingSimulationController.class)
class PricingApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "REQUEST_INVALID",
                "Request validation failed",
                request);
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "REQUEST_INVALID",
                "Request body is malformed or contains an unsupported value",
                request);
    }

    @ExceptionHandler(PricingRuleViolationException.class)
    ProblemDetail handlePricingRule(PricingRuleViolationException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "PRICING_RULE_VIOLATION",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(FxRateUnavailableException.class)
    ProblemDetail handleFxUnavailable(FxRateUnavailableException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "FX_RATE_UNAVAILABLE",
                exception.getMessage(),
                request);
    }

    private static ProblemDetail problem(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("urn:problem:" + code.toLowerCase().replace('_', '-')));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty(
                "correlationId",
                request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
        return problem;
    }
}
