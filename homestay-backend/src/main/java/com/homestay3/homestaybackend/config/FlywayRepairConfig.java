package com.homestay3.homestaybackend.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Flyway 启动配置：在 migrate 之前自动执行 repair，
 * 用于自动清理因开发中 SQL 反复修改导致的 failed migration 记录。
 * 仅 dev 环境生效，生产环境应移除此配置。
 */
@Configuration
@Profile("dev")
public class FlywayRepairConfig {

    @Bean
    @Primary
    public FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
        flyway.repair();
        return new FlywayMigrationInitializer(flyway);
    }
}
