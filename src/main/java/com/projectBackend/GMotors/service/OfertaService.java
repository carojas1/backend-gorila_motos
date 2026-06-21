package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.model.UsuarioRol;
import com.projectBackend.GMotors.repository.UsuarioRepository;
import com.projectBackend.GMotors.repository.UsuarioRolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OfertaService {

    @Autowired
    private UsuarioRolRepository usuarioRolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ResendEmailService resendEmailService;

    public OfertaResultado enviarOferta(String asunto, String mensaje, List<Integer> roles) {
        List<String> destinatarios = new ArrayList<>();

        for (Integer idRol : roles) {
            List<UsuarioRol> relaciones = usuarioRolRepository.findByIdRolAndEstado(idRol, 1);
            for (UsuarioRol ur : relaciones) {
                usuarioRepository.findById(ur.getIdUsuario()).ifPresent(u -> {
                    String correo = u.getCorreo();
                    if (correo != null && !correo.isBlank()
                            && !correo.endsWith("@gmotors.local")
                            && !destinatarios.contains(correo)) {
                        destinatarios.add(correo);
                    }
                });
            }
        }

        AtomicInteger enviados = new AtomicInteger(0);
        AtomicInteger errores  = new AtomicInteger(0);

        for (String correo : destinatarios) {
            try {
                boolean ok = resendEmailService.enviarOfertaMarketing(correo, asunto, mensaje);
                if (ok) enviados.incrementAndGet();
                else    errores.incrementAndGet();
            } catch (Exception e) {
                System.err.println("[OFERTA] Error enviando a " + correo + ": " + e.getMessage());
                errores.incrementAndGet();
            }
        }

        return new OfertaResultado(destinatarios.size(), enviados.get(), errores.get());
    }

    public record OfertaResultado(int total, int enviados, int errores) {}
}
