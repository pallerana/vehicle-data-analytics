package com.solera.interview.service;

import com.solera.interview.analytics.GroupByStrategy;
import com.solera.interview.analytics.TrendAggregateRow;
import com.solera.interview.analytics.VehicleTrendFilter;
import com.solera.interview.dto.analytics.AdminDataStatusDto;
import com.solera.interview.dto.analytics.FilterOptionsDto;
import com.solera.interview.dto.analytics.InsightDto;
import com.solera.interview.dto.analytics.TrendPointDto;
import com.solera.interview.dto.analytics.TrendResponseDto;
import com.solera.interview.dto.analytics.TrendSeriesDto;
import com.solera.interview.repository.VehicleAnalyticsRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class VehicleAnalyticsServiceImpl implements VehicleAnalyticsService {

    private final VehicleAnalyticsRepository repository;
    private final CacheManager cacheManager;

    public VehicleAnalyticsServiceImpl(VehicleAnalyticsRepository repository, CacheManager cacheManager) {
        this.repository = repository;
        this.cacheManager = cacheManager;
    }

    @Override
    @Cacheable(
            value = "vehicleTrends",
            key = "T(java.lang.String).format('%d:%d:%s:%s:%s:%s:%s:%s:%s', #fromYear, #toYear, #groupBy, #fuel, #make, #genModel, #model, #bodyType, #licenceStatus)"
    )
    public TrendResponseDto getTrends(
            int fromYear,
            int toYear,
            String groupBy,
            String fuel,
            String make,
            String genModel,
            String model,
            String bodyType,
            String licenceStatus
    ) {
        GroupByStrategy strategy = GroupByStrategy.fromValue(groupBy);
        int safeFromYear = normalizeFromYear(fromYear);
        int safeToYear = normalizeToYear(toYear);
        if (safeFromYear > safeToYear) {
            int temp = safeFromYear;
            safeFromYear = safeToYear;
            safeToYear = temp;
        }

        VehicleTrendFilter filter = new VehicleTrendFilter(
                safeFromYear,
                safeToYear,
                strategy,
                fuel,
                make,
                genModel,
                model,
                bodyType,
                licenceStatus
        );

        List<TrendAggregateRow> rows = repository.fetchYearlyTrend(filter);
        Map<String, List<TrendAggregateRow>> grouped = new LinkedHashMap<>();
        for (TrendAggregateRow row : rows) {
            grouped.computeIfAbsent(row.category(), key -> new ArrayList<>()).add(row);
        }

        List<TrendSeriesDto> series = grouped.entrySet().stream()
                .map(entry -> toSeries(entry.getKey(), entry.getValue()))
                .toList();

        return new TrendResponseDto(filter.fromYear(), filter.toYear(), strategy.key(), series);
    }

    @Override
    public List<InsightDto> getHighlights(
            int fromYear,
            int toYear,
            String groupBy,
            String fuel,
            String make,
            String genModel,
            String model,
            String bodyType,
            String licenceStatus
    ) {
        TrendResponseDto trendResponse = getTrends(
                fromYear, toYear, groupBy, fuel, make, genModel, model, bodyType, licenceStatus
        );
        if (trendResponse.series().isEmpty()) {
            return List.of(new InsightDto("No matching data", "Try broadening filters or year range."));
        }

        List<TrendSeriesDto> sortedByLastYearTotal = trendResponse.series().stream()
                .sorted(Comparator.comparingLong(this::latestTotal).reversed())
                .toList();

        TrendSeriesDto topCategory = sortedByLastYearTotal.get(0);
        TrendPointDto latestPoint = topCategory.points().stream()
                .max(Comparator.comparingInt(TrendPointDto::year))
                .orElse(null);

        List<InsightDto> insights = new ArrayList<>();
        if (latestPoint != null) {
            insights.add(new InsightDto(
                    "Top category in latest year",
                    "%s leads with %,d vehicles in %d."
                            .formatted(topCategory.category(), latestPoint.total(), latestPoint.year())
            ));
        }

        trendResponse.series().stream()
                .map(this::latestPointWithYoy)
                .filter(point -> point != null && point.yearOnYearPercent() != null)
                .max(Comparator.comparingDouble(TrendPointDto::yearOnYearPercent))
                .ifPresent(point -> insights.add(new InsightDto(
                        "Strongest YoY growth",
                        "Best growth is %,.2f%% in %d."
                                .formatted(point.yearOnYearPercent(), point.year())
                )));

        long totalLatestYear = trendResponse.series().stream()
                .map(this::latestPoint)
                .filter(point -> point != null)
                .mapToLong(TrendPointDto::total)
                .sum();
        insights.add(new InsightDto(
                "Combined latest-year total",
                "Combined total across shown categories is %,d vehicles.".formatted(totalLatestYear)
        ));

        return insights;
    }

    @Override
    @Cacheable(value = "vehicleFilterOptions", key = "#limit")
    public FilterOptionsDto getFilterOptions(int limit) {
        int safeLimit = Math.max(10, Math.min(limit, 500));
        return new FilterOptionsDto(
                repository.fetchDistinctValues("fuel", safeLimit),
                repository.fetchDistinctValues("make", safeLimit),
                repository.fetchDistinctValues("model", safeLimit)
        );
    }

    @Override
    @Cacheable(value = "vehicleAdminStatus", key = "'status'")
    public AdminDataStatusDto getAdminDataStatus() {
        return repository.fetchDataStatus();
    }

    @Override
    public void refreshAnalyticsCache() {
        repository.clearMetadataCache();
        clearCache("vehicleTrends");
        clearCache("vehicleFilterOptions");
        clearCache("vehicleAdminStatus");
    }

    private TrendSeriesDto toSeries(String category, List<TrendAggregateRow> rows) {
        List<TrendAggregateRow> sorted = rows.stream()
                .sorted(Comparator.comparingInt(TrendAggregateRow::year))
                .toList();

        List<TrendPointDto> points = new ArrayList<>();
        Long previousTotal = null;
        for (TrendAggregateRow row : sorted) {
            Long yoyChange = null;
            Double yoyPercent = null;
            if (previousTotal != null) {
                yoyChange = row.total() - previousTotal;
                if (previousTotal != 0) {
                    yoyPercent = (yoyChange * 100.0) / previousTotal;
                }
            }
            points.add(new TrendPointDto(row.year(), row.total(), yoyChange, yoyPercent));
            previousTotal = row.total();
        }

        return new TrendSeriesDto(category, points);
    }

    private long latestTotal(TrendSeriesDto series) {
        return latestPoint(series) == null ? 0 : latestPoint(series).total();
    }

    private TrendPointDto latestPoint(TrendSeriesDto series) {
        return series.points().stream().max(Comparator.comparingInt(TrendPointDto::year)).orElse(null);
    }

    private TrendPointDto latestPointWithYoy(TrendSeriesDto series) {
        return series.points().stream()
                .filter(point -> point.yearOnYearPercent() != null)
                .max(Comparator.comparingInt(TrendPointDto::year))
                .orElse(null);
    }

    private int normalizeFromYear(int fromYear) {
        return Math.max(1994, Math.min(fromYear, 2025));
    }

    private int normalizeToYear(int toYear) {
        return Math.max(1994, Math.min(toYear, 2025));
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(Objects.requireNonNull(cacheName, "cacheName is required"));
        if (cache != null) {
            cache.clear();
        }
    }
}
