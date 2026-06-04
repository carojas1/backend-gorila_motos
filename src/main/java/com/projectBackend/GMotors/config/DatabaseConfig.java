package com.projectBackend.GMotors.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Convierte URLs de Render/Supabase al formato JDBC que requiere Spring Boot.
 * Soporta: postgres://, postgresql://, jdbc:postgresql://
 */
@Configuration
public class DatabaseConfig {

    @PostConstruct
    public void fixDatabaseUrl() {
        String raw = System.getenv("DB_URL");
        if (raw == null || raw.isBlank()) return;

        String jdbc;
        if (raw.startsWith("jdbc:postgresql://")) {
            // Ya está en formato correcto
            jdbc = raw;
        } else if (raw.startsWith("postgres://") || raw.startsWith("postgresql://")) {
            // Convierte postgres(ql)://user:pass@host/db  →  jdbc:postgresql://host/db
            jdbc = raw
                .replaceFirst("^postgres://",    "jdbc:postgresql://")
                .replaceFirst("^postgresql://",  "jdbc:postgresql://");
            // Elimina user:pass@ del host (Spring usa DB_USER y DB_PASSWORD por separado)
            jdbc = jdbc.replaceFirst("jdbc:postgresql://[^@]+@", "jdbc:postgresql://");
        } else {
            return;
        }

        System.setProperty("spring.datasource.url", jdbc);
        System.out.println("[DB] Datasource URL → " + jdbc.replaceAll(":[^@/]+@", ":***@"));
    }
}
