package com.hybrid9.pg.Lipanasi.component;

import com.hybrid9.pg.Lipanasi.configs.MultiTableDynamicNamingStrategy;
import jakarta.annotation.PostConstruct;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DynamicTableNamingConfigurer {
    private final MultiTableDynamicNamingStrategy namingStrategy;

    @Autowired
    public DynamicTableNamingConfigurer(PhysicalNamingStrategy physicalNamingStrategy) {
        this.namingStrategy = (MultiTableDynamicNamingStrategy) physicalNamingStrategy;
    }

    @PostConstruct
    public void configureDynamicTableNames() {
        // Custom prefixes for table names
        namingStrategy.addDynamicTable("c2b_transaction_details", "c2b_trans_details");
       // namingStrategy.addTableNamePrefix("modern_table", "new_");
    }
}
