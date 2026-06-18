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

        // Recolectamos TODO lo pendiente (no avisado aún) y enviamos UN SOLO correo.
        List<ResendEmailService.ItemMantenimiento> items = new ArrayList<>();
        List<AlertaEnviada> pendientesGuardar = new ArrayList<>();

        for (ParametroMantenimiento p : params) {
            int    intervalo  = p.getIntervaloKm();
            int    umbral     = (kmActual / intervalo) * intervalo; // último múltiplo cruzado

            if (umbral == 0) continue; // moto nueva, aún no alcanzó primer intervalo

            // ── Vencido (km cruzó múltiplo exacto) ──
            String tipoVencido = p.getTipoMantenimiento() + "_VENCIDO";
            if (!alertaRepo.existsByIdMotoAndTipoAndKmUmbral(moto.getId_moto(), tipoVencido, umbral)) {
                items.add(new ResendEmailService.ItemMantenimiento(
                    p.getTipoMantenimiento(), p.getDescripcion(), 0, true));
                pendientesGuardar.add(new AlertaEnviada(moto.getId_moto(), tipoVencido, umbral, LocalDateTime.now()));
            }

            // ── Próximo (dentro del 20% del siguiente intervalo) ──
            int nextUmbral = umbral + intervalo;
            int kmRestante = nextUmbral - kmActual;
            if (kmRestante <= (int)(intervalo * 0.20)) {
                String tipoProximo = p.getTipoMantenimiento() + "_PROXIMO";
                if (!alertaRepo.existsByIdMotoAndTipoAndKmUmbral(moto.getId_moto(), tipoProximo, nextUmbral)) {
                    items.add(new ResendEmailService.ItemMantenimiento(
                        p.getTipoMantenimiento(), p.getDescripcion(), kmRestante, false));
                    pendientesGuardar.add(new AlertaEnviada(moto.getId_moto(), tipoProximo, nextUmbral, LocalDateTime.now()));
                }
            }
        }

        if (items.isEmpty()) return; // nada nuevo que avisar

        // Vencidos primero en la lista (más urgentes arriba)
        items.sort((a, b) -> Boolean.compare(b.vencido, a.vencido));

        emailService.enviarResumenMantenimiento(
            correo, nombre, moto.getPlaca(), moto.getMarca(), moto.getModelo(), kmActual, items);

        // Marcamos como avisado para no reenviar el mismo umbral
        for (AlertaEnviada a : pendientesGuardar) alertaRepo.save(a);
    }
}
