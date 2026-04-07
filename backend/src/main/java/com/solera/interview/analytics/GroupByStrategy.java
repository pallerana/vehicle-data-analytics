package com.solera.interview.analytics;

import java.util.Arrays;

public enum GroupByStrategy {
    TOTAL("total", "'Total'"),
    FUEL("fuel", "v.[Fuel]"),
    MAKE("make", "v.[Make]"),
    GEN_MODEL("genModel", "v.[GenModel]"),
    MODEL("model", "v.[Model]"),
    BODY_TYPE("bodyType", "v.[BodyType]"),
    LICENCE_STATUS("licenceStatus", "v.[LicenceStatus]");

    private final String key;
    private final String sqlExpression;

    GroupByStrategy(String key, String sqlExpression) {
        this.key = key;
        this.sqlExpression = sqlExpression;
    }

    public String key() {
        return key;
    }

    public String sqlExpression() {
        return sqlExpression;
    }

    public static GroupByStrategy fromValue(String value) {
        return Arrays.stream(values())
                .filter(strategy -> strategy.key.equalsIgnoreCase(value))
                .findFirst()
                .orElse(FUEL);
    }
}
