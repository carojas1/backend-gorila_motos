package com.projectBackend.GMotors.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class ResendEmailService {
    @Value("${resend.api-key}")
    private String resendApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    // ✅ AGREGAR PARÁMETRO plataforma
    public boolean enviarEmailRecuperacion(String correoDestino, String tokenRecuperacion, String nombreUsuario, String plataforma) {
        try {
            String enlaceRecuperacion;
            
            // ✅ Cambiar el enlace según la plataforma
            if ("flutter".equalsIgnoreCase(plataforma)) {
                // Para Flutter - deep link
                enlaceRecuperacion = "gmotors://reset-password?token=" + tokenRecuperacion;
            } else {
                // Para Angular - URL web
                enlaceRecuperacion = "http://localhost:4200/restablecer-contrasena?token=" + tokenRecuperacion;
                // Para producción, cambiar a:
                // enlaceRecuperacion = "https://tu-dominio.com/restablecer-contrasena?token=" + tokenRecuperacion;
            }

            String htmlContent = construirHtmlEmail(nombreUsuario, enlaceRecuperacion);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("from", "onboarding@resend.dev");
            body.put("to", correoDestino);
            body.put("subject", "Restablecer tu contraseña - Gorila Motors");
            body.put("html", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(RESEND_API_URL, request, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("Error al enviar email: " + e.getMessage());
            return false;
        }
    }

    private String construirHtmlEmail(String nombreUsuario, String enlaceRecuperacion) {
        return "<!DOCTYPE html><html><body style='font-family: Arial;'>" +
               "<h1>Gorila Motors - Restablece tu contraseña</h1>" +
               "<p>Hola " + nombreUsuario + ",</p>" +
               "<p>Para restablecer tu contraseña, haz clic aquí:</p>" +
               "<a href='" + enlaceRecuperacion + "' style='background-color: #FFC107; color: black; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;'>Recuperar Contraseña</a>" +
               "<p>El Token expira en 1 hora.</p>" +
               "</body></html>";
    }
}