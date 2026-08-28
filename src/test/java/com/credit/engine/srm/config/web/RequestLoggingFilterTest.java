package com.credit.engine.srm.config.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    @Test
    void shouldLogOnlySafeRequestMetadataAndScopeIdempotencyKey() throws Exception {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/settlement-batches");
        request.addHeader("Authorization", "Bearer secret-token");
        request.addHeader("Idempotency-Key", "batch-001");
        request.setContent("{\"faceValue\":\"100000.00\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
                assertThat(MDC.get("idempotencyKey")).isEqualTo("batch-001");
                ((MockHttpServletResponse) ignoredResponse).setStatus(202);
            });
        } finally {
            logger.detachAppender(appender);
            MDC.clear();
        }

        assertThat(MDC.get("idempotencyKey")).isNull();
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getFormattedMessage()).isEqualTo("HTTP request completed");
        assertThat(event.getKeyValuePairs())
                .extracting(pair -> Map.entry(pair.key, String.valueOf(pair.value)))
                .contains(
                        Map.entry("httpMethod", "POST"),
                        Map.entry("path", "/api/v1/settlement-batches"),
                        Map.entry("status", "202"));
        assertThat(event.toString())
                .doesNotContain("secret-token")
                .doesNotContain("100000.00");
    }
}
