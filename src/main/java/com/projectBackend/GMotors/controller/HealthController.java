package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.service.ResendEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired(required = false)
    private ResendEmailService emailService;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "GMotors");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("smtp_configured", mailUsername != null && !mailUsername.isBlank());
        response.put("smtp_user", mailUsername != null ? mailUsername : "no configurado");
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint público de prueba de email — llama en el navegador:
     * GET /api/health/email-test?to=tu@correo.com
     * Devuelve ok:true si el correo se envió, ok:false + error si falló.
     */
    @GetMapping("/health/email-test")
    public ResponseEntity<Map<String, Object>> emailTest(
            @RequestParam(defaultValue = "gorilamotos2026@gmail.com") String to) {

        Map<String, Object> result = new HashMap<>();
        result.put("to", to);
        result.put("smtp_user", mailUsername != null ? mailUsername : "vacío");
        result.put("smtp_configured", mailUsername != null && !mailUsername.isBlank());

        if (emailService == null) {
            result.put("ok", false);
            result.put("error", "ResendEmailService no inyectado");
            return ResponseEntity.ok(result);
        }
        try {
            boolean sent = emailService.enviarEmailPrueba(to);
            result.put("ok", sent);
            result.put("mensaje", sent ? "Correo enviado — revisa la bandeja (y spam)" : "Envío fallido — revisa logs de Render");
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}