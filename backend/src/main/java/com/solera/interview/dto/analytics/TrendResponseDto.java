package com.solera.interview.dto.analytics;

import java.util.List;

public record TrendResponseDto(
        int fromYear,
        int toYear,
        String groupBy,
        List<TrendSeriesDto> series
) {
}
