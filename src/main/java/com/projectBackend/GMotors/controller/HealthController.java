package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.service.ResendEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired(required = false)
    private ResendEmailService emailService;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "GMotors");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("smtp_configured", mailUsername != null && !mailUsername.isBlank());
        response.put("smtp_user", mailUsername != null ? mailUsername : "no configurado");
        response.put("brevo_configured", brevoApiKey != null && !brevoApiKey.isBlank());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/email-test")
    public ResponseEntity<Map<String, Object>> emailTest(
            @RequestParam(defaultValue = "gorilamotos2026@gmail.com") String to) {

        Map<String, Object> result = new HashMap<>();
        result.put("to", to);
        result.put("smtp_configured", mailUsername != null && !mailUsername.isBlank());
        result.put("brevo_configured", brevoApiKey != null && !brevoApiKey.isBlank());

        /* Prueba directa de Brevo para ver el error exacto */
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            try {
                RestTemplate rt = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("api-key", brevoApiKey);

                Map<String, Object> body = new HashMap<>();
                body.put("sender", Map.of("email", "gorilamotos2026@gmail.com", "name", "Gorila Motos"));
                body.put("to", List.of(Map.of("email", to)));
                body.put("subject", "✓ Prueba Brevo — Gorila Motos");
                body.put("htmlContent", "<p>Email de prueba enviado desde Render via Brevo API.</p>");

                HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
                ResponseEntity<String> res = rt.postForEntity("https://api.brevo.com/v3/smtp/email", req, String.class);

                result.put("ok", res.getStatusCode().is2xxSuccessful());
                result.put("brevo_status", res.getStatusCode().value());
                result.put("brevo_response", res.getBody());
                result.put("mensaje", res.getStatusCode().is2xxSuccessful()
                        ? "Correo enviado por Brevo — revisa la bandeja (y spam)"
                        : "Brevo respondió con error " + res.getStatusCode().value());
                return ResponseEntity.ok(result);

            } catch (HttpClientErrorException e) {
                result.put("ok", false);
                result.put("brevo_status", e.getStatusCode().value());
                result.put("brevo_error", e.getResponseBodyAsString());
                result.put("mensaje", "Brevo rechazó el request — ver brevo_error");
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                result.put("ok", false);
                result.put("brevo_error", e.getMessage());
                result.put("mensaje", "Excepción al llamar Brevo");
                return ResponseEntity.ok(result);
            }
        }

        /* Sin Brevo: intenta el servicio general (SMTP / Resend) */
        if (emailService == null) {
            result.put("ok", false);
            result.put("error", "ResendEmailService no inyectado y BREVO_API_KEY no configurado");
            return ResponseEntity.ok(result);
        }
        try {
            boolean sent = emailService.enviarEmailPrueba(to);
            result.put("ok", sent);
            result.put("mensaje", sent ? "Correo enviado (fallback)" : "Todos los proveedores fallaron");
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}