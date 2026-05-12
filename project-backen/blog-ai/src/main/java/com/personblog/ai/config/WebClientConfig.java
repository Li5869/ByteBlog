package com.personblog.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${ai.python-service.url:http://python-ai}")
    private String pythonServiceUrl;

    @Bean
    @LoadBalanced
    public WebClient.Builder pythonAiWebClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(5));

        return WebClient.builder()
                .baseUrl(pythonServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024));
    }

    @Bean
    public WebClient pythonAiWebClient(WebClient.Builder pythonAiWebClientBuilder) {
        return pythonAiWebClientBuilder.build();
    }
}
