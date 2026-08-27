package com.credit.engine.srm.receivables.internal.adapter.in.web;

import com.credit.engine.srm.config.web.ApiProblems;
import com.credit.engine.srm.receivables.internal.application.AssignorDocumentAlreadyExistsException;
import com.credit.engine.srm.receivables.internal.application.AssignorNotFoundException;
import com.credit.engine.srm.receivables.internal.application.ReceivableRuleViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AssignorController.class, ReceivableController.class})
class ReceivablesApiExceptionHandler {

    @ExceptionHandler(AssignorDocumentAlreadyExistsException.class)
    ProblemDetail handleDuplicate(AssignorDocumentAlreadyExistsException exception, HttpServletRequest request) {
        return ApiProblems.create(
                HttpStatus.CONFLICT, "ASSIGNOR_DOCUMENT_ALREADY_EXISTS", exception.getMessage(), request);
    }

    @ExceptionHandler(AssignorNotFoundException.class)
    ProblemDetail handleNotFound(AssignorNotFoundException exception, HttpServletRequest request) {
        return ApiProblems.create(HttpStatus.NOT_FOUND, "ASSIGNOR_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(ReceivableRuleViolationException.class)
    ProblemDetail handleRule(ReceivableRuleViolationException exception, HttpServletRequest request) {
        return ApiProblems.create(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "RECEIVABLE_RULE_VIOLATION",
                exception.getMessage(),
                request);
    }
}
