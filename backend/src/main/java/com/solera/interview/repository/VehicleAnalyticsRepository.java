package com.solera.interview.repository;

import com.solera.interview.analytics.TrendAggregateRow;
import com.solera.interview.analytics.VehicleTrendFilter;
import com.solera.interview.dto.analytics.AdminDataStatusDto;
import java.util.List;

public interface VehicleAnalyticsRepository {

    List<TrendAggregateRow> fetchYearlyTrend(VehicleTrendFilter filter);

    List<String> fetchDistinctValues(String columnName, int limit);

    AdminDataStatusDto fetchDataStatus();

    void clearMetadataCache();
}
