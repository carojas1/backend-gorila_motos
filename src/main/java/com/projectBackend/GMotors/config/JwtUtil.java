package com.projectBackend.GMotors.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;


@Component
public class JwtUtil {

    private final String SECRET_KEY = "fJ8yX9mL4pQ2vT7sA1bK3rN6eP4cZ8hW0tD9uR5yM2fL8oQ9wE7rB3kU6nH5jC2"; // Cambiar esto despues...
    private final long EXPIRATION = 1000 * 60 * 60 * 4; // 4 horas

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generarToken(String correo) {
        return Jwts.builder()
                .setSubject(correo) 
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extraerCorreo(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
