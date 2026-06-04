package com.projectBackend.GMotors.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Convierte la URL de Render (postgres://...) al formato JDBC (jdbc:postgresql://...)
 * que requiere Spring Boot / HikariCP automáticamente en el arranque.
 */
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @PostConstruct
    public void fixDatabaseUrl() {
        String raw = System.getenv("DB_URL");
        if (raw != null && raw.startsWith("postgres://")) {
            String jdbc = raw.replace("postgres://", "jdbc:postgresql://");
            System.setProperty("spring.datasource.url", jdbc);
            System.out.println("[DB] URL convertida a JDBC: " + jdbc.replaceAll(":([^@]+)@", ":***@"));
        } else if (raw != null) {
            System.setProperty("spring.datasource.url", raw);
        }
    }
}
