package com.credit.engine.srm.currency.internal.adapter.in.web;

import com.credit.engine.srm.config.web.ApiProblems;
import com.credit.engine.srm.currency.internal.application.CurrentExchangeRateNotFoundException;
import com.credit.engine.srm.currency.internal.application.ExchangeRateProviderUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ExchangeRateController.class)
class CurrencyApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidPair(IllegalArgumentException exception, HttpServletRequest request) {
        return ApiProblems.create(
                HttpStatus.BAD_REQUEST,
                "REQUEST_INVALID",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(ExchangeRateProviderUnavailableException.class)
    ProblemDetail handleProviderUnavailable(
            ExchangeRateProviderUnavailableException exception,
            HttpServletRequest request) {
        return ApiProblems.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "FX_PROVIDER_UNAVAILABLE",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(CurrentExchangeRateNotFoundException.class)
    ProblemDetail handleNotFound(CurrentExchangeRateNotFoundException exception, HttpServletRequest request) {
        return ApiProblems.create(
                HttpStatus.NOT_FOUND,
                "FX_RATE_NOT_FOUND",
                exception.getMessage(),
                request);
    }
}
