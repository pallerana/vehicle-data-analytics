package com.solera.interview.dto.analytics;

public record AdminDataStatusDto(
        long totalRows,
        int distinctMakes,
        int minYear,
        int maxYear
) {
}
