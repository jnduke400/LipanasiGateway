package com.hybrid9.pg.Lipanasi.resources;

import com.hybrid9.pg.Lipanasi.dto.activity.PluginStatusInfo;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PluginAnalyticsResource {
    private static final Map<String, PluginStatusInfo> STATUS_MAP = new HashMap<>();

    static {
        STATUS_MAP.put("PLUGIN_LOADED", new PluginStatusInfo("2000", "Plugin initialized and loaded successfully."));
        STATUS_MAP.put("USER_OPENED_PLUGIN", new PluginStatusInfo("2001", "User opened or triggered the payment plugin."));
        STATUS_MAP.put("USER_CLOSED_PLUGIN", new PluginStatusInfo("2002", "User manually closed the plugin before completing the payment."));
        STATUS_MAP.put("USER_STARTED_PAYMENT", new PluginStatusInfo("2003", "User initiated a payment process."));
        STATUS_MAP.put("USER_CANCELLED_PAYMENT", new PluginStatusInfo("2004", "User canceled the payment intentionally."));
        STATUS_MAP.put("PAYMENT_COMPLETED", new PluginStatusInfo("2005", "Payment was completed successfully."));

        STATUS_MAP.put("CLIENT_ERROR", new PluginStatusInfo("4000", "An issue occurred on the client side, such as a bad request."));
        STATUS_MAP.put("INVALID_PAYMENT_DATA", new PluginStatusInfo("4001", "Provided payment data was incomplete or invalid."));
        STATUS_MAP.put("UI_RENDER_ERROR", new PluginStatusInfo("4002", "Plugin user interface failed to render properly in the browser."));
        STATUS_MAP.put("MULTIPLE_PAYMENT_ATTEMPTS", new PluginStatusInfo("4003", "User attempted to pay multiple times in a single session."));

        STATUS_MAP.put("SERVER_ERROR", new PluginStatusInfo("5000", "A backend/server-side error occurred."));
        STATUS_MAP.put("PLUGIN_LOADING_TIMEOUT", new PluginStatusInfo("5001", "Plugin took too long to load (e.g., more than 5–10 seconds)."));
        STATUS_MAP.put("NETWORK_FAILURE", new PluginStatusInfo("5002", "Network issue during plugin usage (e.g., no internet or timeout)."));
        STATUS_MAP.put("UNKNOWN_ERROR", new PluginStatusInfo("5003", "An unspecified or unexpected error occurred."));
    }

    public PluginStatusInfo getStatusInfo(String status) {
        return STATUS_MAP.getOrDefault(
                status,
                new PluginStatusInfo("N/A", "Unknown plugin status")
        );
    }
}
