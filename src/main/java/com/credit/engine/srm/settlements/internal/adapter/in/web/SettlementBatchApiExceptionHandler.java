package com.credit.engine.srm.settlements.internal.adapter.in.web;

import com.credit.engine.srm.config.web.ApiProblems;
import com.credit.engine.srm.settlements.IdempotencyConflictException;
import com.credit.engine.srm.settlements.InvalidSettlementBatchException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = SettlementBatchController.class)
class SettlementBatchApiExceptionHandler {

    @ExceptionHandler(InvalidSettlementBatchException.class)
    ProblemDetail handleInvalidBatch(
            InvalidSettlementBatchException exception,
            HttpServletRequest request) {
        return ApiProblems.create(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "SETTLEMENT_BATCH_INVALID",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail handleIdempotencyConflict(
            IdempotencyConflictException exception,
            HttpServletRequest request) {
        return ApiProblems.create(
                HttpStatus.CONFLICT,
                exception.code(),
                exception.getMessage(),
                request);
    }
}
