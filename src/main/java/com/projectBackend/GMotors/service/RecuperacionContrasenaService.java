package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.model.RecuperacionContrasena;
import com.projectBackend.GMotors.model.Usuario;
import com.projectBackend.GMotors.repository.RecuperacionContrasenaRepository;
import com.projectBackend.GMotors.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class RecuperacionContrasenaService {

    @Autowired
    private RecuperacionContrasenaRepository recuperacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ResendEmailService resendEmailService;

    private static final int TOKEN_EXPIRATION_MINUTES = 60;

    // ✅ AGREGAR parámetro plataforma
    public boolean generarYEnviarRecuperacion(String correo, String plataforma) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo);
            
            if (usuarioOpt.isEmpty()) {
                return true;
            }

            Usuario usuario = usuarioOpt.get();
            String token = generarToken();
            
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime expiracion = ahora.plusMinutes(TOKEN_EXPIRATION_MINUTES);
      
            RecuperacionContrasena recuperacion = new RecuperacionContrasena();
            recuperacion.setIdUsuario(usuario.getId_usuario());
            recuperacion.setToken(token);
            recuperacion.setCorreo(correo);
            recuperacion.setFechaCreacion(ahora);
            recuperacion.setFechaExpiracion(expiracion);
            recuperacion.setUtilizado(false);

            RecuperacionContrasena guardado = recuperacionRepository.save(recuperacion);
            // ✅ PASAR plataforma aquí
            boolean emailEnviado = resendEmailService.enviarEmailRecuperacion(
                correo, 
                token, 
                usuario.getNombre_completo(),
                plataforma
            );

            return emailEnviado;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean validarToken(String token) {
        try {
            Optional<RecuperacionContrasena> recuperacionOpt = recuperacionRepository.findByToken(token);
            
            if (recuperacionOpt.isEmpty()) {
                return false;
            }

            RecuperacionContrasena recuperacion = recuperacionOpt.get();
            boolean esValido = recuperacion.esValido();
            
            return esValido;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean restablecerContrasena(String token, String nuevaContrasena) {
        try {
            Optional<RecuperacionContrasena> recuperacionOpt = recuperacionRepository.findByToken(token);
            
            if (recuperacionOpt.isEmpty()) {
                return false;
            }

            RecuperacionContrasena recuperacion = recuperacionOpt.get();
            
            if (!recuperacion.esValido()) {
                return false;
            }

            Optional<Usuario> usuarioOpt = usuarioRepository.findById(recuperacion.getIdUsuario());
            
            if (usuarioOpt.isEmpty()) {
                return false;
            }

            Usuario usuario = usuarioOpt.get();

            String contrasenaCriptada = passwordEncoder.encode(nuevaContrasena);
            usuario.setContrasena(contrasenaCriptada);
            usuarioRepository.save(usuario);

            recuperacion.setUtilizado(true);
            recuperacion.setFechaUtilizacion(LocalDateTime.now());
            recuperacionRepository.save(recuperacion);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String generarToken() {
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        return token;
    }
}