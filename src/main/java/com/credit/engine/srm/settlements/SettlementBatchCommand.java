package com.credit.engine.srm.settlements;

import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ReceivableId;

import java.util.List;
import java.util.Objects;

public record SettlementBatchCommand(List<Item> items) {

    public SettlementBatchCommand {
        items = List.copyOf(Objects.requireNonNull(items, "items is required"));
    }

    public record Item(ReceivableId receivableId, Currency paymentCurrency) {

        public Item {
            Objects.requireNonNull(receivableId, "receivableId is required");
            Objects.requireNonNull(paymentCurrency, "paymentCurrency is required");
        }
    }
}
