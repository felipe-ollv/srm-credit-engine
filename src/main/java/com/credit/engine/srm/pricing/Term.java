package com.credit.engine.srm.pricing;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record Term(int months) {

    public static final int MAX_MONTHS = 360;

    public Term {
        if (months < 1 || months > MAX_MONTHS) {
            throw new IllegalArgumentException("term must be between 1 and 360 months");
        }
    }

    public static Term between(LocalDate pricingDate, LocalDate dueDate) {
        Objects.requireNonNull(pricingDate, "pricingDate is required");
        Objects.requireNonNull(dueDate, "dueDate is required");

        if (!dueDate.isAfter(pricingDate)) {
            throw new IllegalArgumentException("dueDate must be after pricingDate");
        }

        long completeMonths = ChronoUnit.MONTHS.between(pricingDate, dueDate);
        LocalDate completeMonthsDate = pricingDate.plusMonths(completeMonths);
        long startedMonths = completeMonths + (completeMonthsDate.isBefore(dueDate) ? 1 : 0);

        if (startedMonths > MAX_MONTHS) {
            throw new IllegalArgumentException("term cannot exceed 360 months");
        }

        return new Term(Math.toIntExact(startedMonths));
    }
}
