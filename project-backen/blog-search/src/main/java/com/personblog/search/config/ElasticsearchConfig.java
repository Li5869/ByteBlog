package com.personblog.search.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 配置
 *
 * 只有当 search.enabled=true 时才启用 ES 相关 Bean
 * 所有 ES 操作统一使用 ElasticsearchOperations，不再使用 Repository
 * 避免在 ES 不可达时导致应用启动失败
 */
@Configuration
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchConfig {
}
