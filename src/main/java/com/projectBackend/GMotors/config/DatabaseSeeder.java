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

            // ─── PARÁMETROS DE MANTENIMIENTO (Estudio Ecuador — ISO 9000) ─────────
            // Intervalos basados en condiciones de rodadura ecuatorianas:
            // altitud (Quito 2800m afecta filtros/bujías), clima cálido (costa),
            // calidad de combustible 87-octanos, marcas populares AKT/Shineray/Honda/Yamaha.
            if (parametroRepo.count() == 0) {

                // 50-125cc: scooters, AKT, Shineray, Ranger — uso urbano intensivo
                seedParam(parametroRepo, 50, 125,  "ACEITE",           2000, "Aceite motor — cambio cada 2 000 km (50-125cc)");
                seedParam(parametroRepo, 50, 125,  "FILTRO_AIRE",      4000, "Filtro de aire — limpieza/cambio cada 4 000 km");
                seedParam(parametroRepo, 50, 125,  "BUJIA",            5000, "Bujía — inspección y cambio cada 5 000 km");
                seedParam(parametroRepo, 50, 125,  "CADENA",           3000, "Cadena/correa — tensado y lubricación cada 3 000 km");
                seedParam(parametroRepo, 50, 125,  "LLANTA_TRASERA",   8000, "Llanta trasera — revisión de profundidad cada 8 000 km");
                seedParam(parametroRepo, 50, 125,  "FRENOS",           5000, "Pastillas y líquido de frenos — inspección cada 5 000 km");
                seedParam(parametroRepo, 50, 125,  "REVISION_GENERAL", 6000, "Revisión general completa cada 6 000 km");

                // 126-200cc: Honda CB190, Yamaha FZ-S, AKT TT200 — uso mixto
                seedParam(parametroRepo, 126, 200, "ACEITE",           3000, "Aceite motor — cambio cada 3 000 km (126-200cc)");
                seedParam(parametroRepo, 126, 200, "FILTRO_AIRE",      6000, "Filtro de aire — limpieza/cambio cada 6 000 km");
                seedParam(parametroRepo, 126, 200, "BUJIA",            8000, "Bujía — inspección y cambio cada 8 000 km");
                seedParam(parametroRepo, 126, 200, "CADENA",           5000, "Cadena — tensado y lubricación cada 5 000 km");
                seedParam(parametroRepo, 126, 200, "LLANTA_TRASERA",  10000, "Llanta trasera — revisión cada 10 000 km");
                seedParam(parametroRepo, 126, 200, "FRENOS",           7000, "Frenos — inspección completa cada 7 000 km");
                seedParam(parametroRepo, 126, 200, "REVISION_GENERAL", 8000, "Revisión general cada 8 000 km");

                // 201-400cc: Yamaha MT-03, Honda CB300R, Bajaj Dominar
                seedParam(parametroRepo, 201, 400, "ACEITE",           4000, "Aceite motor — cambio cada 4 000 km (201-400cc)");
                seedParam(parametroRepo, 201, 400, "FILTRO_AIRE",      8000, "Filtro de aire — cada 8 000 km");
                seedParam(parametroRepo, 201, 400, "BUJIA",           10000, "Bujía de iridio — cada 10 000 km");
                seedParam(parametroRepo, 201, 400, "CADENA",           8000, "Cadena — tensado y lubricación cada 8 000 km");
                seedParam(parametroRepo, 201, 400, "LLANTA_TRASERA",  12000, "Llanta trasera — revisión cada 12 000 km");
                seedParam(parametroRepo, 201, 400, "FRENOS",           9000, "Frenos — inspección cada 9 000 km");
                seedParam(parametroRepo, 201, 400, "REVISION_GENERAL",10000, "Revisión general cada 10 000 km");

                // 401-650cc: Yamaha MT-07, KTM Duke 390, Honda CB500F
                seedParam(parametroRepo, 401, 650, "ACEITE",           5000, "Aceite sintético — cambio cada 5 000 km (401-650cc)");
                seedParam(parametroRepo, 401, 650, "FILTRO_AIRE",     10000, "Filtro de aire — cada 10 000 km");
                seedParam(parametroRepo, 401, 650, "BUJIA",           12000, "Bujía de platino/iridio — cada 12 000 km");
                seedParam(parametroRepo, 401, 650, "CADENA",          10000, "Cadena — lubricación y revisión cada 10 000 km");
                seedParam(parametroRepo, 401, 650, "LLANTA_TRASERA",  15000, "Llanta trasera — desgaste cada 15 000 km");
                seedParam(parametroRepo, 401, 650, "FRENOS",          11000, "Sistema de frenos — cada 11 000 km");
                seedParam(parametroRepo, 401, 650, "REVISION_GENERAL",12000, "Revisión general cada 12 000 km");

                // 651cc+: Honda CB1000R, Yamaha MT-09, BMW GS — alta cilindrada
                seedParam(parametroRepo, 651, null, "ACEITE",           6000, "Aceite 100% sintético — cambio cada 6 000 km (651cc+)");
                seedParam(parametroRepo, 651, null, "FILTRO_AIRE",     12000, "Filtro de aire de alto flujo — cada 12 000 km");
                seedParam(parametroRepo, 651, null, "BUJIA",           15000, "Bujía de iridio — cada 15 000 km");
                seedParam(parametroRepo, 651, null, "CADENA",          12000, "Cadena reforzada — cada 12 000 km");
                seedParam(parametroRepo, 651, null, "LLANTA_TRASERA",  18000, "Llanta trasera de alto rendimiento — cada 18 000 km");
                seedParam(parametroRepo, 651, null, "FRENOS",          13000, "Discos y pastillas — cada 13 000 km");
                seedParam(parametroRepo, 651, null, "REVISION_GENERAL",15000, "Revisión general integral cada 15 000 km");

                System.out.println("✅ Parámetros de mantenimiento Ecuador cargados (35 filas)");
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
