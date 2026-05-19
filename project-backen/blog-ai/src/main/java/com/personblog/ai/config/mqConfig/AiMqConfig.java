package com.personblog.ai.config.mqConfig;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiMqConfig {
    public static final String AI_TITLE_QUEUE = "ai_title_queue";

    public static final String AI_EXCHANGE = "ai_exchange";

    public static final String AI_TITLE_KEY = "ai.title.key";

    public static final String AI_MODERATE_QUEUE = "ai_moderate_queue";

    public static final String AI_MODERATE_KEY = "ai.moderate.key";

    public static final String AI_DLX = "ai.dlx";
    public static final String AI_TITLE_DLQ = "ai_title_dlq";
    public static final String AI_TITLE_DLK = "ai.title.dlk";
    public static final String AI_MODERATE_DLQ = "ai_moderate_dlq";
    public static final String AI_MODERATE_DLK = "ai.moderate.dlk";

    @Bean
    public Queue AiTitleQueue(){
        return QueueBuilder.durable(AI_TITLE_QUEUE)
                .withArgument("x-dead-letter-exchange", AI_DLX)
                .withArgument("x-dead-letter-routing-key", AI_TITLE_DLK)
                .build();
    }

    @Bean
    public DirectExchange AiExchange(){
        return new DirectExchange(AI_EXCHANGE,true,false);
    }

    @Bean
    public Binding AiTitleBinding(){
        return BindingBuilder.bind(AiTitleQueue()).to(AiExchange()).with(AI_TITLE_KEY);
    }

    @Bean
    public Queue AiModerateQueue(){
        return QueueBuilder.durable(AI_MODERATE_QUEUE)
                .withArgument("x-dead-letter-exchange", AI_DLX)
                .withArgument("x-dead-letter-routing-key", AI_MODERATE_DLK)
                .build();
    }

    @Bean
    public Binding AiModerateBinding(){
        return BindingBuilder.bind(AiModerateQueue()).to(AiExchange()).with(AI_MODERATE_KEY);
    }

    @Bean
    public DirectExchange aiDlx() {
        return new DirectExchange(AI_DLX, true, false);
    }

    @Bean
    public Queue aiTitleDlq() {
        return QueueBuilder.durable(AI_TITLE_DLQ).build();
    }

    @Bean
    public Binding aiTitleDlqBinding() {
        return BindingBuilder.bind(aiTitleDlq()).to(aiDlx()).with(AI_TITLE_DLK);
    }

    @Bean
    public Queue aiModerateDlq() {
        return QueueBuilder.durable(AI_MODERATE_DLQ).build();
    }

    @Bean
    public Binding aiModerateDlqBinding() {
        return BindingBuilder.bind(aiModerateDlq()).to(aiDlx()).with(AI_MODERATE_DLK);
    }
}
