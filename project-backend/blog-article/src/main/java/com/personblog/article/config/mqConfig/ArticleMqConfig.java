package com.personblog.article.config.mqConfig;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArticleMqConfig {
    public static final String ARTICLE_TO_ES_QUEUE = "article_es_queue";
    public static final String ARTICLE_EXCHANGE = "article_exchange";
    public static final String ARTICLE_TO_ES_KEY = "article.es.key";

    public static final String ARTICLE_DLX = "article.dlx";
    public static final String ARTICLE_ES_DLQ = "article_es_dlq";
    public static final String ARTICLE_ES_DLK = "article.es.dlk";

    @Bean
    public Queue ArticleEsQueue(){
        return QueueBuilder.durable(ARTICLE_TO_ES_QUEUE)
                .withArgument("x-dead-letter-exchange", ARTICLE_DLX)
                .withArgument("x-dead-letter-routing-key", ARTICLE_ES_DLK)
                .build();
    }
    @Bean
    public DirectExchange ArticleExchange(){
        return new DirectExchange(ARTICLE_EXCHANGE,true,false);
    }
    @Bean
    public Binding ArticleEsBinding(){
        return BindingBuilder.bind(ArticleEsQueue()).to(ArticleExchange()).with(ARTICLE_TO_ES_KEY);
    }

    @Bean
    public DirectExchange articleDlx() {
        return new DirectExchange(ARTICLE_DLX, true, false);
    }

    @Bean
    public Queue articleEsDlq() {
        return QueueBuilder.durable(ARTICLE_ES_DLQ).build();
    }

    @Bean
    public Binding articleEsDlqBinding() {
        return BindingBuilder.bind(articleEsDlq()).to(articleDlx()).with(ARTICLE_ES_DLK);
    }
}
