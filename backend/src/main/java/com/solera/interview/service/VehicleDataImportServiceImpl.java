package com.solera.interview.service;

import com.solera.interview.dto.analytics.AdminImportResponseDto;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VehicleDataImportServiceImpl implements VehicleDataImportService {

    private static final String TARGET_TABLE = "vehicles_data";
    private static final Pattern MULTIPLE_UNDERSCORE = Pattern.compile("_+");
    private static final Set<String> TEXT_COLUMNS = Set.of(
            "BodyType", "Make", "GenModel", "Model", "Fuel", "LicenceStatus"
    );

    private final JdbcTemplate jdbcTemplate;
    private final VehicleAnalyticsService analyticsService;

    public VehicleDataImportServiceImpl(JdbcTemplate jdbcTemplate, VehicleAnalyticsService analyticsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.analyticsService = analyticsService;
    }

    @Override
    @Transactional
    public AdminImportResponseDto importVehiclesCsv(MultipartFile file) {
        validateFile(file);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV file is empty.");
            }

            List<String> originalHeaders = parseCsvLine(removeBom(headerLine));
            if (originalHeaders.size() < 7) {
                throw new IllegalArgumentException("CSV header is invalid. Expected dimension and quarter columns.");
            }

            List<String> dbColumns = normalizeHeaders(originalHeaders);
            recreateTable(dbColumns, originalHeaders);

            String insertSql = buildInsertSql(dbColumns.size());
            long importedRows = insertRows(reader, originalHeaders, dbColumns, insertSql);

            analyticsService.refreshAnalyticsCache();
            return new AdminImportResponseDto(
                    "CSV import completed successfully.",
                    importedRows,
                    dbColumns.size()
            );
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read uploaded CSV file.", exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a non-empty CSV file.");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Only .csv files are supported.");
        }
    }

    private String removeBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private List<String> normalizeHeaders(List<String> originalHeaders) {
        List<String> normalized = new ArrayList<>();
        Set<String> used = new HashSet<>();

        for (String header : originalHeaders) {
            String candidate = normalizeColumnName(header);
            String uniqueName = candidate;
            int suffix = 2;
            while (used.contains(uniqueName.toLowerCase(Locale.ROOT))) {
                uniqueName = candidate + "_" + suffix;
                suffix += 1;
            }
            used.add(uniqueName.toLowerCase(Locale.ROOT));
            normalized.add(uniqueName);
        }
        return normalized;
    }

    private String normalizeColumnName(String source) {
        String value = source == null ? "" : source.trim();
        if (value.isEmpty()) {
            value = "column";
        }
        value = value.replaceAll("[^A-Za-z0-9_ ]", "_");
        value = value.replace(' ', '_');
        value = MULTIPLE_UNDERSCORE.matcher(value).replaceAll("_");
        value = value.replaceAll("^_+", "");
        value = value.replaceAll("_+$", "");
        if (value.isEmpty()) {
            value = "column";
        }
        if (Character.isDigit(value.charAt(0))) {
            value = "_" + value;
        }
        return value;
    }

    private void recreateTable(List<String> dbColumns, List<String> originalHeaders) {
        StringBuilder createSql = new StringBuilder();
        createSql.append("IF OBJECT_ID('dbo.").append(TARGET_TABLE).append("','U') IS NOT NULL DROP TABLE dbo.")
                .append(TARGET_TABLE).append("; ");
        createSql.append("CREATE TABLE dbo.").append(TARGET_TABLE).append(" (");

        for (int index = 0; index < dbColumns.size(); index += 1) {
            String dbColumn = dbColumns.get(index);
            String originalColumn = originalHeaders.get(index).trim();
            String sqlType = TEXT_COLUMNS.contains(originalColumn) ? "NVARCHAR(255) NULL" : "INT NULL";
            if (index > 0) {
                createSql.append(", ");
            }
            createSql.append("[").append(dbColumn).append("] ").append(sqlType);
        }
        createSql.append(");");

        jdbcTemplate.execute(Objects.requireNonNull(createSql.toString()));
    }

    private String buildInsertSql(int columnCount) {
        StringBuilder placeholders = new StringBuilder();
        for (int index = 0; index < columnCount; index += 1) {
            if (index > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }
        return "INSERT INTO dbo." + TARGET_TABLE + " VALUES (" + placeholders + ")";
    }

    private long insertRows(
            BufferedReader reader,
            List<String> originalHeaders,
            List<String> dbColumns,
            String insertSql
    ) throws IOException {
        final int batchSize = 500;
        long importedRows = 0L;
        int lineNumber = 1;
        List<List<String>> batch = new ArrayList<>(batchSize);

        String line;
        while ((line = reader.readLine()) != null) {
            lineNumber += 1;
            if (line.isBlank()) {
                continue;
            }

            List<String> values = parseCsvLine(line);
            if (values.size() != dbColumns.size()) {
                throw new IllegalArgumentException(
                        "CSV row has unexpected column count at line " + lineNumber + "."
                );
            }
            batch.add(values);

            if (batch.size() >= batchSize) {
                importedRows += writeBatch(insertSql, batch, originalHeaders);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            importedRows += writeBatch(insertSql, batch, originalHeaders);
        }
        return importedRows;
    }

    private int writeBatch(String insertSql, List<List<String>> batch, List<String> originalHeaders) {
        int[] result = jdbcTemplate.batchUpdate(Objects.requireNonNull(insertSql), new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NonNull PreparedStatement preparedStatement, int i) throws SQLException {
                List<String> row = batch.get(i);
                for (int columnIndex = 0; columnIndex < row.size(); columnIndex += 1) {
                    String rawValue = row.get(columnIndex);
                    String originalColumn = originalHeaders.get(columnIndex).trim();
                    if (TEXT_COLUMNS.contains(originalColumn)) {
                        String text = normalizeText(rawValue);
                        if (text == null) {
                            preparedStatement.setNull(columnIndex + 1, Types.NVARCHAR);
                        } else {
                            preparedStatement.setString(columnIndex + 1, text);
                        }
                    } else {
                        Integer numericValue = parseInteger(rawValue);
                        if (numericValue == null) {
                            preparedStatement.setNull(columnIndex + 1, Types.INTEGER);
                        } else {
                            preparedStatement.setInt(columnIndex + 1, numericValue);
                        }
                    }
                }
            }

            @Override
            public int getBatchSize() {
                return batch.size();
            }
        });

        int written = 0;
        for (int value : result) {
            if (value >= 0) {
                written += value;
            } else {
                written += 1;
            }
        }
        return written;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private Integer parseInteger(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid numeric value in CSV: '" + cleaned + "'.");
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index += 1) {
            char currentChar = line.charAt(index);
            if (currentChar == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index += 1;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (currentChar == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(currentChar);
            }
        }

        values.add(current.toString());
        return values;
    }
}
