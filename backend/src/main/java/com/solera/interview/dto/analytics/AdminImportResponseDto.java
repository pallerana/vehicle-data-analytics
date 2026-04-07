package com.solera.interview.dto.analytics;

public record AdminImportResponseDto(
        String message,
        long importedRows,
        int detectedColumns
) {
}
