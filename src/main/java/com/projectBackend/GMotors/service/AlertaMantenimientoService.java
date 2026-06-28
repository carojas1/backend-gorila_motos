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
    @Autowired private MantenimientoRealizadoRepository mantenimientoRepo;

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
            
            // 1. Obtener el último mantenimiento real de la base de datos
            List<MantenimientoRealizado> mantenimientos = mantenimientoRepo.findByIdMotoAndTipo(moto.getId_moto(), p.getTipoMantenimiento());
            
            int ultimoUmbral = 0; // Fallback si no hay mantenimientos (no debería pasar con las motos nuevas)
            if (mantenimientos != null && !mantenimientos.isEmpty()) {
                ultimoUmbral = mantenimientos.stream()
                        .mapToInt(MantenimientoRealizado::getKmServicio)
                        .max()
                        .orElse(0);
            }

            int proximoUmbral   = ultimoUmbral + intervalo;
            int kmDesdeUltimo   = Math.max(0, kmActual - ultimoUmbral);
            int porcentaje      = Math.min(100, (int) ((double) kmDesdeUltimo / intervalo * 100));
            int kmRestante      = Math.max(0, proximoUmbral - kmActual);

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

        // Detectamos si hay ALGO NUEVO que cruzó umbral (para decidir si enviar).
        // El antirrebote por (moto, tipo, km_umbral) evita reenviar el mismo umbral
        // aunque el usuario suba/baje el kilometraje (no se gasta el cupo de correos).
        List<AlertaEnviada> pendientesGuardar = new ArrayList<>();

        for (ParametroMantenimiento p : params) {
            int intervalo = p.getIntervaloKm();
            
            // Buscar el último mantenimiento real para basar la alerta
            List<MantenimientoRealizado> mantenimientos = mantenimientoRepo.findByIdMotoAndTipo(moto.getId_moto(), p.getTipoMantenimiento());
            int ultimoUmbral = 0;
            if (mantenimientos != null && !mantenimientos.isEmpty()) {
                ultimoUmbral = mantenimientos.stream()
                        .mapToInt(MantenimientoRealizado::getKmServicio)
                        .max()
                        .orElse(0);
            }
            int proximoUmbral = ultimoUmbral + intervalo;
            int kmDesdeUltimo = Math.max(0, kmActual - ultimoUmbral);
            
            if (proximoUmbral == 0) continue; // Evitar spam si algo falla, pero permitir que motos nuevas (ultimoUmbral == 0) reciban alertas

            String tipoVencido = p.getTipoMantenimiento() + "_VENCIDO";
            if (kmDesdeUltimo >= intervalo && !alertaRepo.existsByIdMotoAndTipoAndKmUmbral(moto.getId_moto(), tipoVencido, proximoUmbral)) {
                pendientesGuardar.add(new AlertaEnviada(moto.getId_moto(), tipoVencido, proximoUmbral, LocalDateTime.now()));
            }

            int kmRestante = proximoUmbral - kmActual;
            if (kmRestante > 0 && kmRestante <= (int)(intervalo * 0.20)) {
                String tipoProximo = p.getTipoMantenimiento() + "_PROXIMO";
                if (!alertaRepo.existsByIdMotoAndTipoAndKmUmbral(moto.getId_moto(), tipoProximo, proximoUmbral)) {
                    pendientesGuardar.add(new AlertaEnviada(moto.getId_moto(), tipoProximo, proximoUmbral, LocalDateTime.now()));
                }
            }
        }

        if (pendientesGuardar.isEmpty()) return; // nada nuevo cruzó umbral

        // UN SOLO correo con el DETALLE TÉCNICO COMPLETO: todos los componentes con su %.
        List<EstadoMantenimiento> full = calcularEstado(moto);
        List<ResendEmailService.ItemMantenimiento> todos = new ArrayList<>();
        for (EstadoMantenimiento e : full) {
            todos.add(new ResendEmailService.ItemMantenimiento(
                e.tipo, e.descripcion, e.porcentajeDesgaste, e.kmRestante, e.estado));
        }

        emailService.enviarResumenMantenimiento(
            correo, nombre, moto.getPlaca(), moto.getMarca(), moto.getModelo(), kmActual, todos);

        // Marcamos como avisado para no reenviar el mismo umbral
        for (AlertaEnviada a : pendientesGuardar) alertaRepo.save(a);
    }
}
