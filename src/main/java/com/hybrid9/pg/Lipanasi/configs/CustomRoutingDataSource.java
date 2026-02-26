package com.hybrid9.pg.Lipanasi.configs;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class CustomRoutingDataSource extends AbstractRoutingDataSource {
    private static final ThreadLocal<String> currentDataSource = new ThreadLocal<>();

    @Override
    protected Object determineCurrentLookupKey() {
        return currentDataSource.get();
    }

    public static void setCurrentDataSource(String dataSourceName) {
        currentDataSource.set(dataSourceName);
    }

    public static void clearCurrentDataSource() {
        currentDataSource.remove();
    }
}
