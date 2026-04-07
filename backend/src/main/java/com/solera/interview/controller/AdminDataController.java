package com.solera.interview.controller;

import com.solera.interview.dto.analytics.AdminDataStatusDto;
import com.solera.interview.dto.analytics.AdminImportResponseDto;
import com.solera.interview.service.VehicleDataImportService;
import com.solera.interview.service.VehicleAnalyticsService;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/data")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminDataController {

    private final VehicleAnalyticsService analyticsService;
    private final VehicleDataImportService vehicleDataImportService;

    public AdminDataController(
            VehicleAnalyticsService analyticsService,
            VehicleDataImportService vehicleDataImportService
    ) {
        this.analyticsService = analyticsService;
        this.vehicleDataImportService = vehicleDataImportService;
    }

    @GetMapping("/status")
    public AdminDataStatusDto status() {
        return analyticsService.getAdminDataStatus();
    }

    @PostMapping("/refresh")
    public Map<String, String> refresh() {
        analyticsService.refreshAnalyticsCache();
        return Map.of("message", "Analytics cache refreshed");
    }

    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminImportResponseDto importCsv(@RequestPart("file") MultipartFile file) {
        return vehicleDataImportService.importVehiclesCsv(file);
    }
}
