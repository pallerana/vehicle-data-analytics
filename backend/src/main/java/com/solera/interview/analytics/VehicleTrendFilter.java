package com.solera.interview.analytics;

public record VehicleTrendFilter(
        int fromYear,
        int toYear,
        GroupByStrategy groupBy,
        String fuel,
        String make,
        String genModel,
        String model,
        String bodyType,
        String licenceStatus
) {
}
