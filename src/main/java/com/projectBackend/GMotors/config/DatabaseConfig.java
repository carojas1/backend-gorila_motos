package com.projectBackend.GMotors.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Convierte URLs de Render/Supabase al formato JDBC que requiere Spring Boot.
 *
 * IMPORTANTE: Implementa EnvironmentPostProcessor (NO @PostConstruct) para
 * ejecutarse ANTES de que Spring inicialice el DataSource. Con @PostConstruct
 * era demasiado tarde y la URL nunca se aplicaba.
 *
 * Soporta: postgres://, postgresql://, jdbc:postgresql://
 *
 * Registro en: META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class DatabaseConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 1. Intentar DB_URL desde variable de entorno del sistema
        String raw = System.getenv("DB_URL");

        // 2. Intentar desde properties de Spring si no está en env
        if (raw == null || raw.isBlank()) {
            raw = environment.getProperty("DB_URL");
        }

        if (raw == null || raw.isBlank()) {
            // No hay DB_URL configurada; el perfil por defecto usará H2 para desarrollo local
            return;
        }

        String jdbc = convertToJdbcUrl(raw);
        if (jdbc == null) return;

        // Inyectar la URL convertida con máxima prioridad
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", jdbc);
        environment.getPropertySources().addFirst(
            new MapPropertySource("databaseUrlOverride", props)
        );

        // Log seguro (oculta password si está embebida en la URL)
        String safeUrl = jdbc.replaceAll(":[^@/]+@", ":***@");
        System.out.println("[GMotors-DB] DataSource URL → " + safeUrl);
    }

    private String convertToJdbcUrl(String raw) {
        if (raw.startsWith("jdbc:postgresql://")) {
            // Ya está en el formato correcto
            return raw;
        }

        if (raw.startsWith("postgres://") || raw.startsWith("postgresql://")) {
            // Convierte postgres(ql)://user:pass@host/db  →  jdbc:postgresql://host/db
            // Spring Boot usa DB_USER y DB_PASSWORD por separado vía HikariCP
            String jdbc = raw
                .replaceFirst("^postgres://", "jdbc:postgresql://")
                .replaceFirst("^postgresql://", "jdbc:postgresql://");

            // Elimina user:pass@ del host si está embebido
            jdbc = jdbc.replaceFirst("jdbc:postgresql://[^@]+@", "jdbc:postgresql://");
            return jdbc;
        }

        // Formato desconocido — no modificar
        System.out.println("[GMotors-DB] WARNING: DB_URL con formato desconocido, usando tal cual: " + raw.substring(0, Math.min(30, raw.length())) + "...");
        return null;
    }
}
