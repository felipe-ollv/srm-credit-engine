package com.credit.engine.srm.settlements.internal.application;

final class MissingBatchExchangeRateException extends RuntimeException {

    MissingBatchExchangeRateException() {
        super("No current USD/BRL exchange rate is available");
    }
}
