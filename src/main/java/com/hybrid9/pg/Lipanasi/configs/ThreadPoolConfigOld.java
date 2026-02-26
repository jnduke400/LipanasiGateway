package com.hybrid9.pg.Lipanasi.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//@Configuration
public class ThreadPoolConfigOld {

    @Bean("airtelMoneyThreadPool")
    public ExecutorService airtelMoneyThreadPool(){
        return Executors.newFixedThreadPool(5);
    }

    @Bean("mpesaThreadPool")
    public ExecutorService mpesaThreadPool(){
        return Executors.newFixedThreadPool(5);
    }

    @Bean("mixxThreadPool")
    public ExecutorService mixxThreadPool(){
        return Executors.newFixedThreadPool(5);
    }

    @Bean("ttclThreadPool")
    public ExecutorService ttclThreadPool(){
        return Executors.newFixedThreadPool(5);
    }

    @Bean("halopesaThreadPool")
    public ExecutorService haloPesaThreadPool(){
        return Executors.newFixedThreadPool(5);
    }

    @Bean("dlrThreadPool")
    public ExecutorService dlrThreadPool(){
        return Executors.newFixedThreadPool(5);
    }
}
