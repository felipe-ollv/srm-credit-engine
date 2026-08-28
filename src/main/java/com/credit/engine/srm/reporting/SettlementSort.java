package com.credit.engine.srm.reporting;

import java.util.Locale;

public record SettlementSort(Field field, Direction direction) {

    public SettlementSort {
        if (field == null || direction == null) {
            throw new InvalidSettlementSearchException("Sort field and direction are required");
        }
    }

    public static SettlementSort parse(String value) {
        if (value == null || value.isBlank()) {
            return new SettlementSort(Field.SETTLED_AT, Direction.DESC);
        }
        String[] parts = value.split(",", -1);
        if (parts.length != 2) {
            throw new InvalidSettlementSearchException(
                    "sort must use the format field,direction");
        }
        try {
            return new SettlementSort(
                    Field.fromHttp(parts[0]),
                    Direction.valueOf(parts[1].trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw new InvalidSettlementSearchException(
                    "sort supports settledAt, assignorLegalName or paymentAmount with asc or desc");
        }
    }

    public enum Field {
        SETTLED_AT,
        ASSIGNOR_LEGAL_NAME,
        PAYMENT_AMOUNT;

        private static Field fromHttp(String value) {
            return switch (value.trim()) {
                case "settledAt" -> SETTLED_AT;
                case "assignorLegalName" -> ASSIGNOR_LEGAL_NAME;
                case "paymentAmount" -> PAYMENT_AMOUNT;
                default -> throw new IllegalArgumentException("unsupported sort field");
            };
        }
    }

    public enum Direction {
        ASC,
        DESC
    }
}
