package com.hybrid9.pg.Lipanasi.repositories.payments.activity;

import com.hybrid9.pg.Lipanasi.entities.payments.activity.PluginAnalytic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PluginAnalyticRepository extends JpaRepository<PluginAnalytic, Long> {
}
