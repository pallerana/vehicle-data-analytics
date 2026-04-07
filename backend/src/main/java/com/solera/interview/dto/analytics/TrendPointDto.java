package com.solera.interview.dto.analytics;

public record TrendPointDto(
        int year,
        long total,
        Long yearOnYearChange,
        Double yearOnYearPercent
) {
}
