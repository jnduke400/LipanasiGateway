package com.hybrid9.pg.Lipanasi.dto.activity;

import lombok.Builder;

@Builder
public class PluginStatusInfo {
    private String code;
    private String description;

    public PluginStatusInfo(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

