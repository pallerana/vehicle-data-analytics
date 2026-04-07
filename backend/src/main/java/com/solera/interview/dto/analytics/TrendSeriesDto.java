package com.solera.interview.dto.analytics;

import java.util.List;

public record TrendSeriesDto(
        String category,
        List<TrendPointDto> points
) {
}
