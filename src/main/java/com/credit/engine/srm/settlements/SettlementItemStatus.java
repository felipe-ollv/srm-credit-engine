package com.credit.engine.srm.settlements;

public enum SettlementItemStatus {
    SUCCESS,
    NOT_FOUND,
    CONFLICT,
    RULE_VIOLATION,
    FX_RATE_UNAVAILABLE,
    TECHNICAL_ERROR
}
