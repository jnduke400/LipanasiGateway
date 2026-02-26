package com.hybrid9.pg.Lipanasi.serviceImpl.payments.activity;

import com.hybrid9.pg.Lipanasi.entities.payments.activity.PluginAnalytic;
import com.hybrid9.pg.Lipanasi.repositories.payments.activity.PluginAnalyticRepository;
import com.hybrid9.pg.Lipanasi.services.payments.activity.PluginAnalyticService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PluginAnalyticServiceImpl implements PluginAnalyticService {
    private final PluginAnalyticRepository pluginAnalyticRepository;

    @Override
    public PluginAnalytic savePluginAnalytic(PluginAnalytic pluginAnalytic) {
        return pluginAnalyticRepository.save(pluginAnalytic);
    }
}
