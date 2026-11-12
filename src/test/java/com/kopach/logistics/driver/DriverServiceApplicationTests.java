package com.kopach.logistics.driver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

// Старт приложения: миграции применяются, сущности совпадают со схемой (ddl-auto: validate)
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false", // Kafka для этого теста не нужна
        "internal.api-key=test-internal-key"        // ключ только для тестов
})
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
