package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.model.*;
import com.projectBackend.GMotors.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AlertaMantenimientoService {

    @Autowired private ParametroMantenimientoRepository parametroRepo;
    @Autowired private AlertaEnviadaRepository          alertaRepo;
    @Autowired private UsuarioRepository                usuarioRepo;
    @Autowired private ResendEmailService               emailService;

    /* ── DTO de estado de mantenimiento (cálculo, sin BD) ──────────────────── */
    public static class EstadoMantenimiento {
        public String  tipo;
        public String  descripcion;
        public int     intervaloKm;
        public int     kmActual;
        public int     proximoCambioKm;
        public int     kmRestante;
        public int     porcentajeDesgaste;
        public String  estado; // OK / PROXIMO / VENCIDO
    }

    /* ── Calcula el estado de mantenimiento (para el frontend) ─────────────── */
    public List<EstadoMantenimiento> calcularEstado(Moto moto) {
        List<ParametroMantenimiento> params = parametroRepo.findByCc(moto.getCilindraje());
        List<EstadoMantenimiento> resultado = new ArrayList<>();

        for (ParametroMantenimiento p : params) {
            int intervalo       = p.getIntervaloKm();
            int kmActual        = moto.getKilometraje();
            int ultimoUmbral    = (kmActual / intervalo) * intervalo;
            int proximoUmbral   = ultimoUmbral + intervalo;
            int kmDesdeUltimo   = kmActual - ultimoUmbral;
            int porcentaje      = Math.min(100, (int) ((double) kmDesdeUltimo / intervalo * 100));
            int kmRestante      = proximoUmbral - kmActual;

            String estado = porcentaje >= 100 ? "VENCIDO"
                          : porcentaje >= 80  ? "PROXIMO"
                          :                    "OK";

            EstadoMantenimiento em = new EstadoMantenimiento();
            em.tipo                = p.getTipoMantenimiento();
            em.descripcion         = p.getDescripcion();
            em.intervaloKm         = intervalo;
            em.kmActual            = kmActual;
            em.proximoCambioKm     = proximoUmbral;
            em.kmRestante          = kmRestante;
            em.porcentajeDesgaste  = porcentaje;
            em.estado              = estado;
            resultado.add(em);
        }
        return resultado;
    }

    /* ── Verifica y envía alertas (llamado async al actualizar km) ──────────── */
    @Async
    public void verificarYEnviarAlertas(Moto moto) {
        if (moto.getKilometraje() == null || moto.getCilindraje() == null) return;

        Optional<Usuario> usuarioOpt = usuarioRepo.findById(moto.getId_usuario());
        if (usuarioOpt.isEmpty()) return;

        Usuario usuario   = usuarioOpt.get();
        String  correo    = usuario.getCorreo();
        String  nombre    = usuario.getNombre_completo();
        int     kmActual  = moto.getKilometraje();

        List<ParametroMantenimiento> params = parametroRepo.findByCc(moto.getCilindraje());

        for (ParametroMantenimiento p : params) {
            int    intervalo  = p.getIntervaloKm();
            int    umbral     = (kmActual / intervalo) * intervalo; // último múltiplo cruzado

            if (umbral == 0) continue; // moto nueva, aún no alcanzó primer intervalo

            // ── Alerta de vencimiento (km cruzó múltiplo exacto) ──
            String tipoVencido = p.getTipoMantenimiento() + "_VENCIDO";
            if (!alertaRepo.existsByIdMotoAndTipoAndKmUmbral(moto.getId_moto(), tipoVencido, umbral)) {
                emailService.enviarAlertaMantenimientoVencido(
                    correo, nombre, moto.getPlaca(), moto.getMarca(), moto.getModelo(),
                    p.getTipoMantenimiento(), p.getDescripcion(), umbral
                );
                alertaRepo.save(new AlertaEnviada(moto.getId_moto(), tipoVencido, umbral, LocalDateTime.now()));
            }

            // ── Alerta de aproximación (dentro del 20% del próximo intervalo) ──
            int nextUmbral = umbral + intervalo;
            int kmRestante = nextUmbral - kmActual;
            if (kmRestante <= (int)(intervalo * 0.20)) {
                String tipoProximo = p.getTipoMantenimiento() + "_PROXIMO";
                if (!alertaRepo.existsByIdMotoAndTipoAndKmUmbral(moto.getId_moto(), tipoProximo, nextUmbral)) {
                    emailService.enviarAlertaMantenimientoProximo(
                        correo, nombre, moto.getPlaca(), moto.getMarca(), moto.getModelo(),
                        p.getTipoMantenimiento(), p.getDescripcion(), kmRestante, nextUmbral
                    );
                    alertaRepo.save(new AlertaEnviada(moto.getId_moto(), tipoProximo, nextUmbral, LocalDateTime.now()));
                }
            }
        }
    }
}
