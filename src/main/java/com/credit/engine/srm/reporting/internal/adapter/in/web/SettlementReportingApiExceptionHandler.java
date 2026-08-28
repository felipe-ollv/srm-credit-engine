package com.credit.engine.srm.reporting.internal.adapter.in.web;

import com.credit.engine.srm.config.web.ApiProblems;
import com.credit.engine.srm.reporting.InvalidSettlementSearchException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.credit.engine.srm.reporting")
class SettlementReportingApiExceptionHandler {

    @ExceptionHandler(InvalidSettlementSearchException.class)
    ProblemDetail handleInvalidSearch(
            InvalidSettlementSearchException exception,
            HttpServletRequest request) {
        return ApiProblems.create(
                HttpStatus.BAD_REQUEST,
                "REPORT_QUERY_INVALID",
                exception.getMessage(),
                request);
    }
}
