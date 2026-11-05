package com.kopach.logistics.driver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Проверяет, что приложение стартует: все бины создаются, Flyway применяет
 * миграции V1–V2 на пустую базу, а Hibernate (ddl-auto: validate) подтверждает,
 * что сущности совпадают со схемой.
 *
 * База — временная PostgreSQL в Docker (Testcontainers), рабочая БД на 5433 не трогается.
 */
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false") // Kafka для этого теста не нужна
class DriverServiceApplicationTests {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void contextLoads() {
    }
}
