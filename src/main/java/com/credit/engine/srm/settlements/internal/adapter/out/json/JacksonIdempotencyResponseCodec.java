package com.credit.engine.srm.settlements.internal.adapter.out.json;

import com.credit.engine.srm.settlements.SettlementBatchResult;
import com.credit.engine.srm.settlements.internal.application.IdempotencyResponseCodec;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class JacksonIdempotencyResponseCodec implements IdempotencyResponseCodec {

    private final ObjectMapper objectMapper;

    JacksonIdempotencyResponseCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String encode(SettlementBatchResult result) {
        return objectMapper.writeValueAsString(result);
    }

    @Override
    public SettlementBatchResult decode(String payload) {
        return objectMapper.readValue(payload, SettlementBatchResult.class);
    }
}
