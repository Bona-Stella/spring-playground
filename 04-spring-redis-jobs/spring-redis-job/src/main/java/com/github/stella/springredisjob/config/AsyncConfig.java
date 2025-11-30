package com.github.stella.springredisjob.config;

import com.github.stella.springredisjob.common.async.GlobalAsyncExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private final GlobalAsyncExceptionHandler asyncExceptionHandler;

    public AsyncConfig(GlobalAsyncExceptionHandler asyncExceptionHandler) {
        this.asyncExceptionHandler = asyncExceptionHandler;
    }

    // SpringBoot 3.2+ 에서는 spring.threads.virtual.enabled=true 만으로도 기본 executor가 가상 스레드가 되지만,
    // @Async 명시적 실행기를 확실히 하기 위해 Bean을 제공합니다.
    @Bean(name = {"taskExecutor", "applicationTaskExecutor"})
    public TaskExecutor taskExecutor() {
        Executor virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        return new TaskExecutorAdapter(virtualExecutor);
    }

    // @Async(void) 등에서 던져진 예외에 대한 전역 처리 핸들러 등록
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncExceptionHandler;
    }
}
