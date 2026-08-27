package com.credit.engine.srm.receivables.internal.adapter.in.web;

import com.credit.engine.srm.shared.Money;

record MoneyResponseDto(String amount, String currency) {

    static MoneyResponseDto from(Money money) {
        return new MoneyResponseDto(money.amount().toPlainString(), money.currency().name());
    }
}
