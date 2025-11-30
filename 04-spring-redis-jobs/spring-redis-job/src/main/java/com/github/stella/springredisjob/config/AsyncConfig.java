package com.github.stella.springredisjob.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    // @EnableAsync 에서 기본으로 사용하는 TaskExecutor
    @Bean(name = {"taskExecutor", "applicationTaskExecutor"})
    public TaskExecutor taskExecutor() {
        Executor virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        return new TaskExecutorAdapter(virtualExecutor);
    }
}
