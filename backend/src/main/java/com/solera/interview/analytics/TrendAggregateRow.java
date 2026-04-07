package com.solera.interview.analytics;

public record TrendAggregateRow(
        String category,
        int year,
        long total
) {
}
