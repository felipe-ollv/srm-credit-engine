package com.credit.engine.srm.pricing.internal.adapter.in.web;

import com.credit.engine.srm.pricing.internal.application.FxRateUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PricingApiExceptionHandlerTest {

    @Test
    void shouldExposeUnavailableFxAsRetriableProblemDetail() throws Exception {
        PricingSimulationController controller = new PricingSimulationController(
                command -> {
                    throw new FxRateUnavailableException();
                });
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new PricingApiExceptionHandler())
                .addFilters(new CorrelationIdFilter())
                .build();

        mockMvc.perform(post("/api/v1/pricing/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receivableType": "DUPLICATA_MERCANTIL",
                                  "faceValue": "100000.00",
                                  "dueDate": "2026-11-26",
                                  "paymentCurrency": "USD"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FX_RATE_UNAVAILABLE"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }
}
