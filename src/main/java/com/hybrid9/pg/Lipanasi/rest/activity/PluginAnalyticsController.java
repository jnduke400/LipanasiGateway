package com.hybrid9.pg.Lipanasi.rest.activity;

import com.hybrid9.pg.Lipanasi.dto.activity.PluginAnalyticDTO;
import com.hybrid9.pg.Lipanasi.dto.activity.PluginStatusInfo;
import com.hybrid9.pg.Lipanasi.entities.payments.activity.PluginAnalytic;
import com.hybrid9.pg.Lipanasi.resources.PluginAnalyticsResource;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import com.hybrid9.pg.Lipanasi.services.payments.activity.PluginAnalyticService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

//@RestController
//@RequestMapping("/api/v1/plugin-analytics")
@Slf4j
@Validated
@AllArgsConstructor
public class PluginAnalyticsController {
    private final PluginAnalyticService pluginAnalyticsService;
    private final PluginAnalyticsResource pluginAnalyticsResource;
    private final OrderService orderService;

    @Autowired
    @Qualifier("pluginAnalyticsVirtualThread")
    private ExecutorService pluginAnalyticsVirtualThread;

    @PostMapping("/")
    public CompletableFuture<Map<String, Object>> savePluginAnalytics(@Valid @RequestBody PluginAnalyticDTO pluginAnalyticDTO) {
        log.info("Received plugin analytics: {}", pluginAnalyticDTO);
        PluginStatusInfo info = this.pluginAnalyticsResource.getStatusInfo(pluginAnalyticDTO.getPluginStatus());

        // Save plugin analytics to database

        PluginAnalytic pluginAnalytic = PluginAnalytic.builder()
                .pluginName(pluginAnalyticDTO.getPluginName())
                .pluginVersion(pluginAnalyticDTO.getPluginVersion())
                .pluginType(pluginAnalyticDTO.getPluginType())
                .pluginStatus(pluginAnalyticDTO.getPluginStatus())
                .pluginErrorCode(info.getCode())
                .pluginErrorMessage(info.getDescription())
                .activityTime(pluginAnalyticDTO.getActivityTime())
                .order(orderService.findByOrderNumber(pluginAnalyticDTO.getOrder()).orElse(null))
                .build();

        return CompletableFuture.supplyAsync(() -> {

            PluginAnalytic pluginAnalyticResult = this.pluginAnalyticsService.savePluginAnalytic(pluginAnalytic);
            Map<String, Object> response = new HashMap<>();

            if (pluginAnalyticResult != null) {
                response.put("status", "success");
                response.put("message", "Plugin analytics saved successfully");
                response.put("success", true);
                response.put("receivedAt", pluginAnalyticResult.getCreationDate());

            }else{
                response.put("status", "failed");
                response.put("message", "Plugin analytics not saved");
                response.put("success", false);
                response.put("errorCode", "400");
            }
            return response;
        },pluginAnalyticsVirtualThread).exceptionally(e -> {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "failed");
            response.put("message", "Plugin analytics not saved:- "+e.getCause().getMessage());
            response.put("success", false);
            response.put("errorCode", "500");
            return response;
        });

    }
}
