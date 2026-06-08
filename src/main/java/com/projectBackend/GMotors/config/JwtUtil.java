package com.projectBackend.GMotors.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    /**
     * Clave secreta JWT leída desde variable de entorno JWT_SECRET.
     * En producción (Render), configurar esta variable con un string seguro de ≥64 chars.
     * El valor por defecto SOLO se usa en desarrollo local.
     */
    @Value("${jwt.secret:fJ8yX9mL4pQ2vT7sA1bK3rN6eP4cZ8hW0tD9uR5yM2fL8oQ9wE7rB3kU6nH5jC2}")
    private String secretKey;

    @Value("${token.expiration.minutes:120}")
    private long expirationMinutes;

    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        // JJWT requiere mínimo 64 bytes para HS256; padding si es más corto
        if (keyBytes.length < 64) {
            byte[] padded = new byte[64];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            return Keys.hmacShaKeyFor(padded);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generarToken(String correo) {
        long expirationMs = expirationMinutes * 60 * 1000;
        return Jwts.builder()
                .setSubject(correo)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
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
        } catch (ExpiredJwtException e) {
            System.out.println("[JWT] Token expirado para: " + e.getClaims().getSubject());
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}

