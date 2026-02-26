package com.hybrid9.pg.Lipanasi.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class VirtualThreadConfig {
    @Bean(name = "orderProcessorVirtualThread")
    public ExecutorService orderProcessorVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor(); // Auto-scales
    }
    @Bean(name = "depositProcessorVirtualThread")
    public ExecutorService ioExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor(); // Auto-scales
    }

    @Bean(name = "mixxPayBillVirtualThread")
    public ExecutorService mixxPayBillVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor(); // Auto-scales
    }

    @Bean(name = "airtelVirtualThread")
    public ExecutorService airtelVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor(); // Auto-scales
    }

    @Bean(name = "halopesaVirtualThread")
    public ExecutorService halopesaVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor(); // Auto-scales
    }

    @Bean(name = "mixxbyyasVirtualThread")
    public ExecutorService mixxbyyasVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor(); // Auto-scales
    }

    @Bean(name = "mpesaVirtualThread")
    public ExecutorService mpesaVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor(); // Auto-scales
    }

    @Bean(name = "commissionProcessorVirtualThread")
    public ExecutorService commissionProcessorVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor(); // Auto-scales
    }

    @Bean(name = "initDepositVirtualThread")
    public ExecutorService initDepositVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor(); // Auto-scales
    }

    @Bean(name = "completeDepositVirtualThread")
    public ExecutorService completeDepositVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor(); // Auto-scales
    }

    @Bean(name="balanceUpdateVirtualThread")
    public ExecutorService balanceUpdateVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name="depositUpdateVirtualThread")
    public ExecutorService depositUpdateVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    @Bean(name="callbackProcessorVirtualThread")
    public ExecutorService callbackProcessorVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name="crdbVirtualThread")
    public ExecutorService crdbVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name="externalOrderProcessorVirtualThread")
    public ExecutorService externalOrderProcessorVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name="pluginAnalyticsVirtualThread")
    public ExecutorService pluginAnalyticsVirtualThread() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }


}
