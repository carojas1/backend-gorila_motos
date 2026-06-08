package com.projectBackend.GMotors.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.projectBackend.GMotors.model.*;
import com.projectBackend.GMotors.repository.*;

@Configuration
public class DatabaseSeeder {

    @Bean
    CommandLineRunner initDatabase(
            RolRepository rolRepo,
            TipoRepository tipoRepo,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            // ─── ROLES (siempre necesarios) ───────────────────────────────────────
            if (rolRepo.count() == 0) {
                Rol rAdmin = new Rol(); rAdmin.setNombre("ADMIN");    rolRepo.save(rAdmin);
                Rol rCli   = new Rol(); rCli.setNombre("CLIENTE");   rolRepo.save(rCli);
                Rol rMec   = new Rol(); rMec.setNombre("MECANICO");  rolRepo.save(rMec);
                System.out.println("✅ Roles creados: ADMIN, CLIENTE, MECANICO");
            }

            // ─── TIPOS DE MANTENIMIENTO (catálogo base) ───────────────────────────
            if (tipoRepo.count() == 0) {
                seedTipo(tipoRepo, "Mantenimiento General",      "Cambio de aceite, filtro y revisión general del vehículo");
                seedTipo(tipoRepo, "Sistema de Frenos",          "Revisión, limpieza y reemplazo de pastillas y líquido de frenos");
                seedTipo(tipoRepo, "Cadena y Transmisión",       "Limpieza, lubricación y ajuste de cadena, o reemplazo de kit");
                seedTipo(tipoRepo, "Diagnóstico Eléctrico",      "Revisión de batería, alternador, sistema de encendido y luces");
                seedTipo(tipoRepo, "Carburación y Combustible",  "Limpieza de carburador o inyectores, ajuste de mezcla aire-combustible");
                System.out.println("✅ Tipos de mantenimiento creados");
            }

        };
    }

    private void seedTipo(TipoRepository repo, String nombre, String desc) {
        Tipo t = new Tipo();
        t.setNombre(nombre);
        t.setDescripcion(desc);
        repo.save(t);
    }
}
