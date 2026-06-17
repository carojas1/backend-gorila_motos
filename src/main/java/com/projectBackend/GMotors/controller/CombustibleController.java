package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.CargaCombustible;
import com.projectBackend.GMotors.repository.CargaCombustibleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Cargas de combustible (galones) — compartido en la nube.
 */
@RestController
@RequestMapping("/api/combustible")
public class CombustibleController {

    @Autowired
    private CargaCombustibleRepository repo;

    /** Todas las cargas (admin/mecánico) o filtradas por moto. */
    @GetMapping
    public ResponseEntity<List<CargaCombustible>> listar(@RequestParam(value = "moto", required = false) Long idMoto) {
        return ResponseEntity.ok(idMoto != null ? repo.findByIdMoto(idMoto) : repo.findAll());
    }

    @GetMapping("/moto/{idMoto}")
    public ResponseEntity<List<CargaCombustible>> porMoto(@PathVariable Long idMoto) {
        return ResponseEntity.ok(repo.findByIdMoto(idMoto));
    }

    /** Registrar una carga. */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CargaCombustible carga) {
        if (carga.getIdMoto() == null || carga.getFecha() == null || carga.getLitros() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan datos: id_moto, fecha, litros (galones)"));
        }
        carga.setIdCarga(null); // siempre nuevo
        return ResponseEntity.ok(repo.save(carga));
    }

    /** Eliminar una carga. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> borrar(@PathVariable Long id) {
        if (repo.existsById(id)) repo.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Carga eliminada"));
    }
}
