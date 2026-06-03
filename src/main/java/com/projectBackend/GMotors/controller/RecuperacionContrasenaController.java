package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.service.RecuperacionContrasenaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios/recuperacion")
@CrossOrigin(origins = "*")
public class RecuperacionContrasenaController {

    @Autowired
    private RecuperacionContrasenaService recuperacionService;

    @PostMapping("/solicitar")
    public ResponseEntity<Map<String, Object>> solicitarRecuperacion(@RequestBody Map<String, String> request) {
        String correo = request.get("correo");
        String plataforma = request.get("plataforma"); // ✅ Obtener plataforma
        
        if (correo == null || correo.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("exito", false, "mensaje", "El correo es requerido"));
        }
        
        // Si no viene plataforma, asumir "flutter"
        if (plataforma == null || plataforma.trim().isEmpty()) {
            plataforma = "flutter";
        }
        
        // Pasar plataforma al servicio
        boolean resultado = recuperacionService.generarYEnviarRecuperacion(correo.trim(), plataforma);
        
        if (resultado) {
            return ResponseEntity.ok(Map.of("exito", true, "mensaje", "Email enviado si existe el usuario"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("exito", false, "mensaje", "Error al procesar"));
    }

    @PostMapping("/validar-token")
    public ResponseEntity<Map<String, Object>> validarToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("valido", false, "mensaje", "Token requerido"));
        }

        boolean esValido = recuperacionService.validarToken(token.trim());
        
        if (esValido) {
            return ResponseEntity.ok(Map.of("valido", true, "mensaje", "Token válido"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("valido", false, "mensaje", "Token inválido o expirado"));
    }

    @PostMapping("/restablecer")
    public ResponseEntity<Map<String, Object>> restablecerContrasena(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String nuevaContrasena = request.get("nuevaContrasena");

        if (token == null || nuevaContrasena == null) {
            return ResponseEntity.badRequest().body(Map.of("exito", false, "mensaje", "Datos incompletos"));
        }

        if (!validarFortaleza(nuevaContrasena)) {
            return ResponseEntity.badRequest().body(Map.of("exito", false,
                "mensaje", "Contraseña débil (mín 8 caracteres, mayús, minús, números, especiales)"));
        }

        boolean resultado = recuperacionService.restablecerContrasena(token.trim(), nuevaContrasena);
        
        if (resultado) {
            return ResponseEntity.ok(Map.of("exito", true, "mensaje", "Contraseña restablecida"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("exito", false, "mensaje", "Token inválido o expirado"));
    }

    private boolean validarFortaleza(String pwd) {
        return pwd.length() >= 8 &&
               pwd.matches(".*[A-Z].*") &&
               pwd.matches(".*[a-z].*") &&
               pwd.matches(".*[0-9].*") &&
               pwd.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?/].*");
    }
}