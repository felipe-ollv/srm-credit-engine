package com.credit.engine.srm.receivables.internal;

import java.util.Objects;

record Cnpj(String value) {

    Cnpj {
        Objects.requireNonNull(value, "document is required");
        if (!value.matches("(?:\\d{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})")) {
            throw new IllegalArgumentException("document must be a valid CNPJ");
        }
        String normalized = value.replaceAll("[./-]", "");
        if (!normalized.matches("\\d{14}") || allDigitsEqual(normalized) || !hasValidCheckDigits(normalized)) {
            throw new IllegalArgumentException("document must be a valid CNPJ");
        }
        value = normalized;
    }

    static Cnpj of(String value) {
        return new Cnpj(value);
    }

    private static boolean allDigitsEqual(String value) {
        return value.chars().allMatch(character -> character == value.charAt(0));
    }

    private static boolean hasValidCheckDigits(String value) {
        return checkDigit(value, 12) == Character.digit(value.charAt(12), 10)
                && checkDigit(value, 13) == Character.digit(value.charAt(13), 10);
    }

    private static int checkDigit(String value, int length) {
        int[] weights = length == 12
                ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += Character.digit(value.charAt(index), 10) * weights[index];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
