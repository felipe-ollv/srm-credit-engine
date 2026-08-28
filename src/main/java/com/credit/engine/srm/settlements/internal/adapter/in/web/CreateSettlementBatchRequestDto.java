package com.credit.engine.srm.settlements.internal.adapter.in.web;

import com.credit.engine.srm.settlements.SettlementBatchCommand;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ReceivableId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

record CreateSettlementBatchRequestDto(
        @NotNull @Size(min = 1, max = 100) List<@Valid @NotNull Item> items) {

    SettlementBatchCommand toCommand() {
        return new SettlementBatchCommand(items.stream()
                .map(item -> new SettlementBatchCommand.Item(
                        new ReceivableId(item.receivableId()), item.paymentCurrency()))
                .toList());
    }

    record Item(
            @NotNull UUID receivableId,
            @NotNull Currency paymentCurrency) {
    }
}
