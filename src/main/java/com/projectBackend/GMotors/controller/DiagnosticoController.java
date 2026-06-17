package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.*;
import com.projectBackend.GMotors.repository.MotoRepository;
import com.projectBackend.GMotors.repository.UsuarioRepository;
import com.projectBackend.GMotors.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class DiagnosticoController {

    @Autowired private DiagnosticoService            diagnosticoService;
    @Autowired private AlertaMantenimientoService    alertaService;
    @Autowired private MotoRepository                motoRepo;
    @Autowired private UsuarioRepository             usuarioRepo;
    @Autowired private ResendEmailService            emailService;

    /* ── Crear diagnóstico (mecánico) ── */
    @PostMapping("/diagnosticos")
    public ResponseEntity<?> crear(@RequestBody DiagnosticoMoto diagnostico) {
        try {
            DiagnosticoMoto creado = diagnosticoService.crear(diagnostico);
            // Tras guardar: enviar reporte al cliente + disparar alertas de km
            motoRepo.findById(creado.getIdMoto()).ifPresent(moto -> {
                // 1. Correo automático con el resultado del diagnóstico
                try {
                    usuarioRepo.findById(moto.getId_usuario()).ifPresent(dueno ->
                        emailService.enviarDiagnostico(
                            dueno.getCorreo(), dueno.getNombre_completo(),
                            moto.getPlaca(), moto.getMarca(), moto.getModelo(),
                            creado.getKilometrajeIngreso() != null ? creado.getKilometrajeIngreso() : moto.getKilometraje(),
                            creado.getObservacionesGenerales(), creado.getDetalles()
                        ));
                } catch (Exception ignore) { /* el correo no debe bloquear el guardado */ }
                // 2. Alertas de mantenimiento por km (async)
                alertaService.verificarYEnviarAlertas(moto);
            });
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /* ── Historial de diagnósticos de una moto ── */
    @GetMapping("/diagnosticos/moto/{idMoto}")
    public ResponseEntity<List<DiagnosticoMoto>> historialPorMoto(@PathVariable Long idMoto) {
        return ResponseEntity.ok(diagnosticoService.historialPorMoto(idMoto));
    }

    /* ── Diagnósticos del mecánico autenticado ── */
    @GetMapping("/diagnosticos/mecanico/{idMecanico}")
    public ResponseEntity<List<DiagnosticoMoto>> porMecanico(@PathVariable Long idMecanico) {
        return ResponseEntity.ok(diagnosticoService.porMecanico(idMecanico));
    }

    /* ── Detalle de un diagnóstico ── */
    @GetMapping("/diagnosticos/{id}")
    public ResponseEntity<DiagnosticoMoto> detalle(@PathVariable Long id) {
        Optional<DiagnosticoMoto> d = diagnosticoService.buscarPorId(id);
        return d.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /* ── Estado de mantenimiento de una moto (sin emails) ── */
    @GetMapping("/alertas/moto/{idMoto}")
    public ResponseEntity<?> estadoMantenimiento(@PathVariable Long idMoto) {
        Optional<Moto> motoOpt = motoRepo.findById(idMoto);
        if (motoOpt.isEmpty()) return ResponseEntity.notFound().build();
        List<AlertaMantenimientoService.EstadoMantenimiento> estado =
            alertaService.calcularEstado(motoOpt.get());
        return ResponseEntity.ok(estado);
    }

    /* ── Disparar verificación manual de alertas ── */
    @PostMapping("/alertas/verificar/{idMoto}")
    public ResponseEntity<?> verificarAlertas(@PathVariable Long idMoto) {
        Optional<Moto> motoOpt = motoRepo.findById(idMoto);
        if (motoOpt.isEmpty()) return ResponseEntity.notFound().build();
        alertaService.verificarYEnviarAlertas(motoOpt.get());
        return ResponseEntity.ok(Map.of("mensaje", "Verificación de alertas disparada"));
    }
}
