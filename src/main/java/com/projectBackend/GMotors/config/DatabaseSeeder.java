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
            ParametroMantenimientoRepository parametroRepo,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            // ─── ROLES ───────────────────────────────────────────────────────────
            if (rolRepo.count() == 0) {
                Rol rAdmin = new Rol(); rAdmin.setNombre("ADMIN");    rolRepo.save(rAdmin);
                Rol rCli   = new Rol(); rCli.setNombre("CLIENTE");   rolRepo.save(rCli);
                Rol rMec   = new Rol(); rMec.setNombre("MECANICO");  rolRepo.save(rMec);
                System.out.println("✅ Roles creados: ADMIN, CLIENTE, MECANICO");
            }

            // ─── TIPOS DE MANTENIMIENTO ───────────────────────────────────────────
            if (tipoRepo.count() == 0) {
                seedTipo(tipoRepo, "Mantenimiento General",      "Cambio de aceite, filtro y revisión general del vehículo");
                seedTipo(tipoRepo, "Sistema de Frenos",          "Revisión, limpieza y reemplazo de pastillas y líquido de frenos");
                seedTipo(tipoRepo, "Cadena y Transmisión",       "Limpieza, lubricación y ajuste de cadena, o reemplazo de kit");
                seedTipo(tipoRepo, "Diagnóstico Eléctrico",      "Revisión de batería, alternador, sistema de encendido y luces");
                seedTipo(tipoRepo, "Carburación y Combustible",  "Limpieza de carburador o inyectores, ajuste de mezcla aire-combustible");
                System.out.println("✅ Tipos de mantenimiento creados");
            }

            // ─── PARÁMETROS DE MANTENIMIENTO (Estudio Ecuador — preventivo) ──────
            // Intervalos CORTOS a propósito: condiciones de rodadura ecuatorianas
            // (polvo, altitud Quito 2800m, combustible 87-octanos, tráfico urbano)
            // exigen servicio frecuente. Esto mantiene la moto en estado óptimo y
            // dispara recordatorios al correo del cliente más seguido.
            // Se RE-SIEMBRA en cada arranque (deleteAll + insert) para que, si se
            // ajustan los intervalos en código, queden vigentes tras el redeploy
            // SIN intervención manual ni SQL.
            parametroRepo.deleteAll();
            {

                // 50-125cc: AKT, Shineray, Ranger, scooters — uso urbano intensivo
                seedParam(parametroRepo, 50, 125,  "ACEITE",           1200, "Aceite motor — cambio cada 1 200 km (50-125cc)");
                seedParam(parametroRepo, 50, 125,  "FILTRO_AIRE",      2500, "Filtro de aire — limpieza/cambio cada 2 500 km");
                seedParam(parametroRepo, 50, 125,  "BUJIA",            3000, "Bujía — inspección y cambio cada 3 000 km");
                seedParam(parametroRepo, 50, 125,  "CADENA",           1800, "Cadena/correa — tensado y lubricación cada 1 800 km");
                seedParam(parametroRepo, 50, 125,  "LLANTA_TRASERA",   5000, "Llanta trasera — revisión de profundidad cada 5 000 km");
                seedParam(parametroRepo, 50, 125,  "FRENOS",           3000, "Pastillas y líquido de frenos — inspección cada 3 000 km");
                seedParam(parametroRepo, 50, 125,  "REVISION_GENERAL", 3500, "Revisión general completa cada 3 500 km");

                // 126-200cc: Honda CB190, Yamaha FZ-S, AKT TT200 — uso mixto
                seedParam(parametroRepo, 126, 200, "ACEITE",           1800, "Aceite motor — cambio cada 1 800 km (126-200cc)");
                seedParam(parametroRepo, 126, 200, "FILTRO_AIRE",      3500, "Filtro de aire — limpieza/cambio cada 3 500 km");
                seedParam(parametroRepo, 126, 200, "BUJIA",            5000, "Bujía — inspección y cambio cada 5 000 km");
                seedParam(parametroRepo, 126, 200, "CADENA",           3000, "Cadena — tensado y lubricación cada 3 000 km");
                seedParam(parametroRepo, 126, 200, "LLANTA_TRASERA",   6000, "Llanta trasera — revisión cada 6 000 km");
                seedParam(parametroRepo, 126, 200, "FRENOS",           4000, "Frenos — inspección completa cada 4 000 km");
                seedParam(parametroRepo, 126, 200, "REVISION_GENERAL", 5000, "Revisión general cada 5 000 km");

                // 201-400cc: Yamaha MT-03, Honda CB300R, Bajaj Dominar
                seedParam(parametroRepo, 201, 400, "ACEITE",           2500, "Aceite motor — cambio cada 2 500 km (201-400cc)");
                seedParam(parametroRepo, 201, 400, "FILTRO_AIRE",      5000, "Filtro de aire — cada 5 000 km");
                seedParam(parametroRepo, 201, 400, "BUJIA",            6000, "Bujía de iridio — cada 6 000 km");
                seedParam(parametroRepo, 201, 400, "CADENA",           5000, "Cadena — tensado y lubricación cada 5 000 km");
                seedParam(parametroRepo, 201, 400, "LLANTA_TRASERA",   7000, "Llanta trasera — revisión cada 7 000 km");
                seedParam(parametroRepo, 201, 400, "FRENOS",           5500, "Frenos — inspección cada 5 500 km");
                seedParam(parametroRepo, 201, 400, "REVISION_GENERAL", 6000, "Revisión general cada 6 000 km");

                // 401-650cc: Yamaha MT-07, KTM Duke 390, Honda CB500F
                seedParam(parametroRepo, 401, 650, "ACEITE",           3000, "Aceite sintético — cambio cada 3 000 km (401-650cc)");
                seedParam(parametroRepo, 401, 650, "FILTRO_AIRE",      6000, "Filtro de aire — cada 6 000 km");
                seedParam(parametroRepo, 401, 650, "BUJIA",            7000, "Bujía de platino/iridio — cada 7 000 km");
                seedParam(parametroRepo, 401, 650, "CADENA",           6000, "Cadena — lubricación y revisión cada 6 000 km");
                seedParam(parametroRepo, 401, 650, "LLANTA_TRASERA",   9000, "Llanta trasera — desgaste cada 9 000 km");
                seedParam(parametroRepo, 401, 650, "FRENOS",           6500, "Sistema de frenos — cada 6 500 km");
                seedParam(parametroRepo, 401, 650, "REVISION_GENERAL", 7000, "Revisión general cada 7 000 km");

                // 651cc+: Honda CB1000R, Yamaha MT-09, BMW GS — alta cilindrada
                seedParam(parametroRepo, 651, null, "ACEITE",           3500, "Aceite 100% sintético — cambio cada 3 500 km (651cc+)");
                seedParam(parametroRepo, 651, null, "FILTRO_AIRE",      7000, "Filtro de aire de alto flujo — cada 7 000 km");
                seedParam(parametroRepo, 651, null, "BUJIA",            9000, "Bujía de iridio — cada 9 000 km");
                seedParam(parametroRepo, 651, null, "CADENA",           7000, "Cadena reforzada — cada 7 000 km");
                seedParam(parametroRepo, 651, null, "LLANTA_TRASERA",  10000, "Llanta trasera de alto rendimiento — cada 10 000 km");
                seedParam(parametroRepo, 651, null, "FRENOS",           7500, "Discos y pastillas — cada 7 500 km");
                seedParam(parametroRepo, 651, null, "REVISION_GENERAL", 9000, "Revisión general integral cada 9 000 km");

                System.out.println("✅ Parámetros de mantenimiento Ecuador re-sembrados (35 filas, intervalos cortos)");
            }
        };
    }

    private void seedTipo(TipoRepository repo, String nombre, String desc) {
        Tipo t = new Tipo();
        t.setNombre(nombre);
        t.setDescripcion(desc);
        repo.save(t);
    }

    private void seedParam(ParametroMantenimientoRepository repo,
                           int ccMin, Integer ccMax, String tipo, int intervalo, String desc) {
        repo.save(new ParametroMantenimiento(ccMin, ccMax, tipo, intervalo, desc));
    }
}
