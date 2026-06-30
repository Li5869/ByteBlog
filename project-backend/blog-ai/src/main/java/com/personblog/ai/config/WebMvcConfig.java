package com.personblog.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executors;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        //虚拟线程
        AsyncTaskExecutor taskExecutor = new TaskExecutorAdapter(
                Executors.newVirtualThreadPerTaskExecutor()
        );
        configurer.setTaskExecutor(taskExecutor);
        configurer.setDefaultTimeout(300000);
    }
}
