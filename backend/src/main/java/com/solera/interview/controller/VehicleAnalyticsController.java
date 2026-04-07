package com.solera.interview.controller;

import com.solera.interview.dto.analytics.FilterOptionsDto;
import com.solera.interview.dto.analytics.InsightDto;
import com.solera.interview.dto.analytics.TrendResponseDto;
import com.solera.interview.service.VehicleAnalyticsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class VehicleAnalyticsController {

    private final VehicleAnalyticsService analyticsService;

    public VehicleAnalyticsController(VehicleAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/trends")
    public TrendResponseDto trends(
            @RequestParam(defaultValue = "2018") @Min(1994) @Max(2025) int fromYear,
            @RequestParam(defaultValue = "2025") @Min(1994) @Max(2025) int toYear,
            @RequestParam(defaultValue = "fuel") String groupBy,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String genModel,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String bodyType,
            @RequestParam(required = false) String licenceStatus
    ) {
        return analyticsService.getTrends(
                fromYear, toYear, groupBy, fuel, make, genModel, model, bodyType, licenceStatus
        );
    }

    @GetMapping("/highlights")
    public List<InsightDto> highlights(
            @RequestParam(defaultValue = "2018") @Min(1994) @Max(2025) int fromYear,
            @RequestParam(defaultValue = "2025") @Min(1994) @Max(2025) int toYear,
            @RequestParam(defaultValue = "fuel") String groupBy,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String genModel,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String bodyType,
            @RequestParam(required = false) String licenceStatus
    ) {
        return analyticsService.getHighlights(
                fromYear, toYear, groupBy, fuel, make, genModel, model, bodyType, licenceStatus
        );
    }

    @GetMapping("/options")
    public FilterOptionsDto options(
            @RequestParam(defaultValue = "100") @Min(10) @Max(500) int limit
    ) {
        return analyticsService.getFilterOptions(limit);
    }
}
