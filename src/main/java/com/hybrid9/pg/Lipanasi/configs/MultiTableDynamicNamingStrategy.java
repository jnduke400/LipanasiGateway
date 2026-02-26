package com.hybrid9.pg.Lipanasi.configs;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

public class MultiTableDynamicNamingStrategy extends PhysicalNamingStrategyStandardImpl {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Map<String, String> DYNAMIC_TABLES = new HashMap<>() {{
        //put("c2b_transaction_details", "c2b_trans_details");
        //put("c2b_ledgers", "audit");
    }};

    @Override
    public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment context) {
        if (logicalName == null) {
            return null;
        }

        String originalName = logicalName.getText();

        // Only transform if it's in our dynamic tables map
        if (DYNAMIC_TABLES.containsKey(originalName)) {
            String currentDate = LocalDate.now().format(DATE_FORMATTER);
            String prefix = DYNAMIC_TABLES.get(originalName);
            String physicalName = prefix + "_" + currentDate;
            return Identifier.toIdentifier(physicalName, logicalName.isQuoted());
        }

        // For all other tables, return the original name
        return logicalName;
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        // Convert camelCase to snake_case for ALL tables' columns
        if (name == null) {
            return null;
        }

        String text = name.getText();
        String newName = text.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        return Identifier.toIdentifier(newName, name.isQuoted());
    }

    public void addDynamicTable(String originalName, String prefix) {
        DYNAMIC_TABLES.put(originalName, prefix);
    }
}