package com.projectBackend.GMotors.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;




@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Permitir preflight CORS
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String path = request.getRequestURI();

        // Rutas públicas — sin verificación de token
        if (path.startsWith("/api/health") ||
        		path.startsWith("/images/") ||
        		path.startsWith("/api/metrics/reports/") ||
                // Healthcheck de Render — crítico para que el servicio no se marque como caído
                path.startsWith("/actuator/") || path.equals("/actuator") ||
        	    path.equals("/api/usuarios/login") ||
        	    (path.equals("/api/usuarios") && request.getMethod().equals("POST")) ||
        	    (path.equals("/api/motos/ocr/placa") && request.getMethod().equals("POST")) ||
        	    (path.equals("/api/usuarios/upload") && request.getMethod().equals("POST")) ||
        	    (path.startsWith("/api/usuarios/recuperacion"))  ||
                (path.equals("/api/quick-accounts/create") && request.getMethod().equals("POST"))) {
                filterChain.doFilter(request, response);
                return;
        	}

        // Extraer token
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            if (!jwtUtil.validarToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":\"Token inv\u00e1lido o expirado\",\"status\":401}");
                return;
            }

            // AUTENTICAR AL USUARIO EN SPRING SECURITY
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken("user", null, List.of());

            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);
            return;
        }

        // Si no hay token → error 401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Se requiere autenticaci\u00f3n\",\"status\":401}");
    }

}
