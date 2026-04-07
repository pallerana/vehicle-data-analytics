package com.solera.interview.service;

import com.solera.interview.dto.analytics.AdminImportResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface VehicleDataImportService {

    AdminImportResponseDto importVehiclesCsv(MultipartFile file);
}
