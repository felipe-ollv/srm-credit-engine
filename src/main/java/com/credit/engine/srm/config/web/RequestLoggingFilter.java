package com.credit.engine.srm.config.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public final class RequestLoggingFilter extends OncePerRequestFilter {

    static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    static final String IDEMPOTENCY_MDC_KEY = "idempotencyKey";
    private static final Pattern VALID_IDEMPOTENCY_KEY =
            Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);
        if (idempotencyKey != null && VALID_IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
            MDC.put(IDEMPOTENCY_MDC_KEY, idempotencyKey);
        }

        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            LOGGER.atInfo()
                    .addKeyValue("httpMethod", request.getMethod())
                    .addKeyValue("path", request.getRequestURI())
                    .addKeyValue("status", response.getStatus())
                    .addKeyValue("durationMs", durationMillis)
                    .log("HTTP request completed");
            MDC.remove(IDEMPOTENCY_MDC_KEY);
        }
    }
}
