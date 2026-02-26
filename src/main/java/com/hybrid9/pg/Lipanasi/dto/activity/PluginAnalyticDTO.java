package com.hybrid9.pg.Lipanasi.dto.activity;

import com.hybrid9.pg.Lipanasi.entities.order.Order;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginAnalyticDTO {
    private String order;
    private String pluginStatus;
    private String activityTime;
    private String pluginVersion;
    private String pluginName;
    private String pluginType;
}
