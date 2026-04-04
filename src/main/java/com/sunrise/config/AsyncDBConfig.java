package com.sunrise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class AsyncDBConfig {

    @Bean("dbExecutor")
    public Executor dbExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1); // Всего один поток для последовательной обработки
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("db-queue-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
