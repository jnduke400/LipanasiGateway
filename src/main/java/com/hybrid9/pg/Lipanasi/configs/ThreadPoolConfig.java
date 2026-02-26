package com.hybrid9.pg.Lipanasi.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    @Value("${thread.pool.core-size}")
    private int coreSize;

    @Value("${thread.pool.max-size}")
    private int maxSize;

    @Value("${thread.pool.queue-capacity}")
    private int queueCapacity;

    @Value("${thread.pool.keep-alive-time}")
    private int keepAliveSeconds;


    @Bean(name = "airtelMoneyThreadPool", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor airtelMoneyThreadPool() {
        return createThreadPool("airtelMoney-", coreSize, queueCapacity, keepAliveSeconds);
    }

    @Bean(name = "airtelMoneyDepositThreadPool", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor airtelMoneyDepositThreadPool() {
        return createThreadPool("airtelMoney-deposit-", coreSize, queueCapacity, keepAliveSeconds);
    }

    @Bean(name = "mpesaThreadPool", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor mpesaThreadPool() {
        return createThreadPool("mpesa-", coreSize, queueCapacity, keepAliveSeconds);
    }

    @Bean(name = "mpesaDepositThreadPool", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor mpesaDepositThreadPool() {
        return createThreadPool("mpesa-deposit-", coreSize, queueCapacity, keepAliveSeconds);
    }

    @Bean(name = "mixxThreadPool", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor mixxThreadPool() {
        return createThreadPool("mixx-", coreSize, queueCapacity, keepAliveSeconds);
    }

    @Bean(name = "mixxDepositThreadPool", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor mixxDepositThreadPool() {
        return createThreadPool("mixx-deposit-", coreSize, queueCapacity, keepAliveSeconds);
    }

    @Bean(name = "tpesaThreadPool", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor tpesaThreadPool() {
        return createThreadPool("tpesa-", coreSize, queueCapacity, keepAliveSeconds);
    }

    @Bean(name = "halopesaThreadPool", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor halopesaThreadPool() {
        return createThreadPool("halopesa-", coreSize, queueCapacity, keepAliveSeconds);
    }

    @Bean(name = "halopesaDepositThreadPool", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor halopesaDepositThreadPool() {
        return createThreadPool("halopesa-deposit-", coreSize, queueCapacity, keepAliveSeconds);
    }

    @Bean(name = "tqsThreadPool", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor tqsThreadPool() {
        return createThreadPool("tqs-", coreSize, queueCapacity, keepAliveSeconds);
    }

    @Bean(name = "crdbThreadPool", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor crdbThreadPool() {
        return createThreadPool("crdb-", coreSize, queueCapacity, keepAliveSeconds);
    }

    @Bean(name = "mixxPayBillThreadPool", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor mixxPayBillThreadPool() {
        return createThreadPool("mixxPayBill-", coreSize, queueCapacity, keepAliveSeconds);
    }

    private ThreadPoolTaskExecutor createThreadPool(String namePrefix, int coreSize, int queueCapacity, int keepAliveSeconds) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(coreSize * 2); // Allow growth during peak times
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(namePrefix);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
