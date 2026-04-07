package com.solera.interview.service;

import com.solera.interview.dto.analytics.AdminDataStatusDto;
import com.solera.interview.dto.analytics.FilterOptionsDto;
import com.solera.interview.dto.analytics.InsightDto;
import com.solera.interview.dto.analytics.TrendResponseDto;
import java.util.List;

public interface VehicleAnalyticsService {

    TrendResponseDto getTrends(
            int fromYear,
            int toYear,
            String groupBy,
            String fuel,
            String make,
            String genModel,
            String model,
            String bodyType,
            String licenceStatus
    );

    List<InsightDto> getHighlights(
            int fromYear,
            int toYear,
            String groupBy,
            String fuel,
            String make,
            String genModel,
            String model,
            String bodyType,
            String licenceStatus
    );

    FilterOptionsDto getFilterOptions(int limit);

    AdminDataStatusDto getAdminDataStatus();

    void refreshAnalyticsCache();
}
