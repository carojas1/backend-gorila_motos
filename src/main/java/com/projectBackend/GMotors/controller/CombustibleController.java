package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.CargaCombustible;
import com.projectBackend.GMotors.model.Moto;
import com.projectBackend.GMotors.repository.CargaCombustibleRepository;
import com.projectBackend.GMotors.service.MotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cargas de combustible (galones) — compartido en la nube.
 * Si km_actual > km actual de la moto, actualiza el odómetro y dispara alertas.
 */
@RestController
@RequestMapping("/api/combustible")
public class CombustibleController {

    @Autowired
    private CargaCombustibleRepository repo;

    @Autowired
    private MotoService motoService;

    /** Todas las cargas (admin/mecánico) o filtradas por moto. */
    @GetMapping
    public ResponseEntity<List<CargaCombustible>> listar(@RequestParam(value = "moto", required = false) Long idMoto) {
        return ResponseEntity.ok(idMoto != null ? repo.findByIdMoto(idMoto) : repo.findAll());
    }

    @GetMapping("/moto/{idMoto}")
    public ResponseEntity<List<CargaCombustible>> porMoto(@PathVariable Long idMoto) {
        return ResponseEntity.ok(repo.findByIdMoto(idMoto));
    }

    /** Registrar una carga. Si km_actual viene, actualiza el odómetro de la moto. */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CargaCombustible carga) {
        if (carga.getIdMoto() == null || carga.getFecha() == null || carga.getLitros() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan datos: id_moto, fecha, litros (galones)"));
        }
        carga.setIdCarga(null);
        CargaCombustible guardada = repo.save(carga);

        // Si viene km_actual, actualizar el odómetro de la moto (dispara alertas de mantenimiento)
        if (carga.getKmActual() != null && carga.getKmActual() > 0) {
            try {
                Optional<Moto> motoOpt = motoService.buscarPorId(carga.getIdMoto());
                if (motoOpt.isPresent()) {
                    Moto moto = motoOpt.get();
                    if (carga.getKmActual() > (moto.getKilometraje() != null ? moto.getKilometraje() : 0)) {
                        Moto patch = new Moto();
                        patch.setKilometraje(carga.getKmActual());
                        motoService.actualizarMoto(moto.getIdMoto(), patch);
                    }
                }
            } catch (Exception e) {
                System.err.println("[COMBUSTIBLE] No se pudo actualizar km de moto " + carga.getIdMoto() + ": " + e.getMessage());
            }
        }

        return ResponseEntity.ok(guardada);
    }

    /** Eliminar una carga. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> borrar(@PathVariable Long id) {
        if (repo.existsById(id)) repo.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Carga eliminada"));
    }
}
