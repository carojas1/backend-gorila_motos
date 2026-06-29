package com.projectBackend.GMotors.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Evita que Spring Boot registre JwtFilter como Servlet filter genérico.
     * Spring Security lo gestiona solo dentro de su cadena de filtros.
     */
    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(cs -> cs.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            // CORS correcto: usa Customizer.withDefaults() para que Spring Security
            // delegue en el CorsConfigurationSource del WebMvcConfigurer registrado en CorsConfig.
            .cors(Customizer.withDefaults())

            // ── Entrypoint explícito: sin token → 401 (no 403 por defecto) ──────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) ->
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Se requiere autenticación"))
                .accessDeniedHandler((req, res, accEx) ->
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado"))
            )

            .authorizeHttpRequests(auth -> auth

                // ============================
                //   RUTAS PÚBLICAS (SIN TOKEN)
                // ============================
                // Healthcheck de Render / UptimeRobot — CUALQUIER método (GET, HEAD, OPTIONS)
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/**").permitAll()
                .requestMatchers("/api/health", "/api/health/**").permitAll()
                .requestMatchers("/api/metrics/reports/**").permitAll()
                .requestMatchers("/api/usuarios/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                .requestMatchers("/images/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/usuarios/upload").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/motos/upload").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/productos/upload").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/motos/ocr/placa").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/motos/ocr/buscar-dueno").permitAll()

                .requestMatchers(HttpMethod.GET,    "/api/rutas/usuario/**").permitAll()
                .requestMatchers(HttpMethod.GET,    "/api/rutas/**").permitAll()
                .requestMatchers(HttpMethod.POST,   "/api/rutas").permitAll()
                .requestMatchers(HttpMethod.PUT,    "/api/rutas/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/rutas/**").permitAll()
                .requestMatchers("/api/usuarios/recuperacion/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/quick-accounts/create").permitAll()

                // ============================
                //   RUTAS PROTEGIDAS
                // ============================
                .requestMatchers("/api/usuarios/**").authenticated()

                // Motos: reglas explícitas por método para evitar ambigüedad con PathPatternParser
                .requestMatchers(HttpMethod.GET,    "/api/motos/**").authenticated()
                .requestMatchers(HttpMethod.GET,    "/api/motos").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/motos").authenticated()
                .requestMatchers(HttpMethod.PUT,    "/api/motos/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/motos/**").authenticated()

                .requestMatchers(HttpMethod.PUT,  "/api/registros/**").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/registros/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/registros/**").authenticated()

                // Diagnósticos y alertas de mantenimiento
                .requestMatchers(HttpMethod.POST, "/api/diagnosticos").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/diagnosticos/**").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/alertas/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/alertas/**").authenticated()

                // Cualquier otra ruta requiere token
                .anyRequest().authenticated()
            )

            // Filtro JWT ANTES del filtro de autenticación estándar
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
}

