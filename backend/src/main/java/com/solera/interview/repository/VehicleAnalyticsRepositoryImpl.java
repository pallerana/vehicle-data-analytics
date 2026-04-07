package com.solera.interview.repository;

import com.solera.interview.analytics.TrendAggregateRow;
import com.solera.interview.analytics.VehicleTrendFilter;
import com.solera.interview.dto.analytics.AdminDataStatusDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class VehicleAnalyticsRepositoryImpl implements VehicleAnalyticsRepository {

    private static final Pattern QUARTER_COLUMN_PATTERN = Pattern.compile("^_?(\\d{4})[_ ]Q([1-4])$");
    private static final String SOURCE_TABLE = "vehicles_data";
    private static final Map<String, String> ALLOWED_FILTER_COLUMNS = Map.of(
            "fuel", "Fuel",
            "make", "Make",
            "genModel", "GenModel",
            "model", "Model",
            "bodyType", "BodyType",
            "licenceStatus", "LicenceStatus"
    );

    private final JdbcTemplate jdbcTemplate;
    private final List<QuarterColumn> quarterColumns = new CopyOnWriteArrayList<>();

    public VehicleAnalyticsRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TrendAggregateRow> fetchYearlyTrend(VehicleTrendFilter filter) {
        ensureQuarterColumnsLoaded();
        String quarterValuesSql = buildQuarterValuesSql(filter.fromYear(), filter.toYear());
        if (quarterValuesSql.isBlank()) {
            return List.of();
        }

        List<Object> parameters = new ArrayList<>();
        boolean isTotalGrouping = filter.groupBy() == com.solera.interview.analytics.GroupByStrategy.TOTAL;
        String categorySelect = isTotalGrouping
                ? "'Total'"
                : "COALESCE(%s, 'Unknown')".formatted(filter.groupBy().sqlExpression());

        StringBuilder sql = new StringBuilder("""
                SELECT
                    %s AS category,
                    period_data.[year] AS [year],
                    SUM(COALESCE(period_data.vehicle_count, 0)) AS total
                FROM dbo.%s v
                CROSS APPLY (VALUES
                %s
                ) period_data([year], [quarter], vehicle_count)
                WHERE 1 = 1
                """.formatted(categorySelect, SOURCE_TABLE, quarterValuesSql));

        appendFilter(sql, parameters, "fuel", filter.fuel());
        appendFilter(sql, parameters, "make", filter.make());
        appendFilter(sql, parameters, "genModel", filter.genModel());
        appendFilter(sql, parameters, "model", filter.model());
        appendFilter(sql, parameters, "bodyType", filter.bodyType());
        appendFilter(sql, parameters, "licenceStatus", filter.licenceStatus());

        if (isTotalGrouping) {
            sql.append("""
                    GROUP BY period_data.[year]
                    ORDER BY period_data.[year] ASC
                    """);
        } else {
            sql.append("""
                    GROUP BY COALESCE(%s, 'Unknown'), period_data.[year]
                    ORDER BY period_data.[year] ASC, category ASC
                    """.formatted(filter.groupBy().sqlExpression()));
        }

        String querySql = Objects.requireNonNull(sql.toString());
        log.info("Executing analytics trend SQL: {} | params={}", querySql, parameters);
        return jdbcTemplate.query(querySql, preparedStatement -> {
            for (int index = 0; index < parameters.size(); index += 1) {
                preparedStatement.setObject(index + 1, parameters.get(index));
            }
        }, (resultSet, rowNum) -> new TrendAggregateRow(
                Objects.requireNonNullElse(resultSet.getString("category"), "Unknown"),
                resultSet.getInt("year"),
                resultSet.getLong("total")
        ));
    }

    @Override
    public List<String> fetchDistinctValues(String columnName, int limit) {
        String dbColumn = ALLOWED_FILTER_COLUMNS.get(columnName);
        if (dbColumn == null) {
            return List.of();
        }

        int safeLimit = Math.max(10, Math.min(limit, 500));
        String sql = """
                SELECT DISTINCT TOP (%d) v.[%s] AS value
                FROM dbo.%s v
                WHERE v.[%s] IS NOT NULL AND LTRIM(RTRIM(v.[%s])) <> ''
                ORDER BY v.[%s]
                """.formatted(safeLimit, dbColumn, SOURCE_TABLE, dbColumn, dbColumn, dbColumn);

        String querySql = Objects.requireNonNull(sql);
        log.info("Executing analytics options SQL: {}", querySql);
        return jdbcTemplate.query(querySql, (resultSet, rowNum) ->
                Objects.requireNonNullElse(resultSet.getString("value"), "")
        ).stream().filter(value -> !value.isBlank()).toList();
    }

    @Override
    public AdminDataStatusDto fetchDataStatus() {
        ensureQuarterColumnsLoaded();
        int minYear = quarterColumns.stream().mapToInt(QuarterColumn::year).min().orElse(0);
        int maxYear = quarterColumns.stream().mapToInt(QuarterColumn::year).max().orElse(0);

        Long totalRows = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM dbo." + SOURCE_TABLE, Long.class);
        Integer distinctMakes = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT [Make]) FROM dbo." + SOURCE_TABLE,
                Integer.class
        );
        log.info("Executing analytics status SQL on table {}", SOURCE_TABLE);

        return new AdminDataStatusDto(
                totalRows == null ? 0L : totalRows,
                distinctMakes == null ? 0 : distinctMakes,
                minYear,
                maxYear
        );
    }

    @Override
    public void clearMetadataCache() {
        synchronized (quarterColumns) {
            quarterColumns.clear();
        }
    }

    private void appendFilter(StringBuilder sql, List<Object> params, String filterKey, String filterValue) {
        if (filterValue == null || filterValue.isBlank()) {
            return;
        }
        String columnName = ALLOWED_FILTER_COLUMNS.get(filterKey);
        if (columnName == null) {
            return;
        }
        sql.append(" AND v.[").append(columnName).append("] = ?");
        params.add(filterValue.trim());
    }

    private void ensureQuarterColumnsLoaded() {
        if (!quarterColumns.isEmpty()) {
            return;
        }
        synchronized (quarterColumns) {
            if (!quarterColumns.isEmpty()) {
                return;
            }

            List<String> columns = jdbcTemplate.queryForList("""
                    SELECT COLUMN_NAME
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ?
                    ORDER BY ORDINAL_POSITION
                    """, String.class, SOURCE_TABLE);

            for (String columnName : columns) {
                if (columnName == null) {
                    continue;
                }
                Matcher matcher = QUARTER_COLUMN_PATTERN.matcher(columnName.trim());
                if (matcher.matches()) {
                    int year = Integer.parseInt(matcher.group(1));
                    int quarter = Integer.parseInt(matcher.group(2));
                    quarterColumns.add(new QuarterColumn(columnName, year, quarter));
                }
            }

            quarterColumns.sort(Comparator.comparingInt(QuarterColumn::year).thenComparingInt(QuarterColumn::quarter));
        }
    }

    private String buildQuarterValuesSql(int fromYear, int toYear) {
        return quarterColumns.stream()
                .filter(column -> column.year() >= fromYear && column.year() <= toYear)
                .map(column -> "(%d, %d, TRY_CONVERT(BIGINT, v.[%s]))"
                        .formatted(column.year(), column.quarter(), column.name()))
                .reduce((left, right) -> left + ",\n" + right)
                .orElse("");
    }

    private record QuarterColumn(String name, int year, int quarter) {
    }
}
