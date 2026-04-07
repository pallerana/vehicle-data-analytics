package com.solera.interview.dto.analytics;

import java.util.List;

public record FilterOptionsDto(
        List<String> fuels,
        List<String> makes,
        List<String> models
) {
}
