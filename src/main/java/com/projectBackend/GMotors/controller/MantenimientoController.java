package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.MantenimientoRealizado;
import com.projectBackend.GMotors.repository.MantenimientoRealizadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Mantenimientos realmente realizados por moto (compartido entre usuarios).
 * El frontend usa esto para resetear el desgaste de una pieza al km del cambio.
 */
@RestController
@RequestMapping("/api/mantenimientos")
public class MantenimientoController {

    @Autowired
    private MantenimientoRealizadoRepository repo;

    /** Lista los mantenimientos registrados de una moto. */
    @GetMapping("/moto/{idMoto}")
    public ResponseEntity<List<MantenimientoRealizado>> porMoto(@PathVariable Long idMoto) {
        return ResponseEntity.ok(repo.findByIdMoto(idMoto));
    }

    /** Registra (o actualiza) el mantenimiento de una pieza al km indicado. */
    @PostMapping
    @Transactional
    public ResponseEntity<?> registrar(@RequestBody MantenimientoRealizado body) {
        if (body.getIdMoto() == null || body.getTipo() == null || body.getKmServicio() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan datos: id_moto, tipo, km_servicio"));
        }
        // Un registro vigente por (moto, tipo): se reemplaza el anterior
        List<MantenimientoRealizado> previos = repo.findByIdMotoAndTipo(body.getIdMoto(), body.getTipo());
        if (!previos.isEmpty()) repo.deleteAll(previos);

        body.setFecha(LocalDate.now());
        return ResponseEntity.ok(repo.save(body));
    }

    /** Deshace el registro de una pieza (vuelve a acumular desgaste). */
    @DeleteMapping("/moto/{idMoto}/{tipo}")
    @Transactional
    public ResponseEntity<?> borrar(@PathVariable Long idMoto, @PathVariable String tipo) {
        List<MantenimientoRealizado> previos = repo.findByIdMotoAndTipo(idMoto, tipo);
        repo.deleteAll(previos);
        return ResponseEntity.ok(Map.of("mensaje", "Registro eliminado"));
    }
}
