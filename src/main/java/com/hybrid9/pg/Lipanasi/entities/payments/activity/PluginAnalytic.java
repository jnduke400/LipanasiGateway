package com.hybrid9.pg.Lipanasi.entities.payments.activity;

import com.hybrid9.pg.Lipanasi.entities.order.Order;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_plugin_analytic", indexes = {@Index(name = "plugin_analytic_plugin_name_idx", columnList = "plugin_name")})
public class PluginAnalytic extends Auditable<String> {
    private String pluginName;
    private String pluginVersion;
    private String pluginType;
    private String pluginStatus;
    private String pluginErrorCode;
    private String pluginErrorMessage;
    private String activityTime;
    @ManyToOne
    private Order order;
}
