package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.CargaCombustible;
import com.projectBackend.GMotors.model.AppConfig;
import com.projectBackend.GMotors.model.Moto;
import com.projectBackend.GMotors.repository.CargaCombustibleRepository;
import com.projectBackend.GMotors.repository.AppConfigRepository;
import com.projectBackend.GMotors.service.MotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
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

    @Autowired
    private AppConfigRepository configRepo;

    /** Todas las cargas (admin/mecánico) o filtradas por moto. */
    @GetMapping
    public ResponseEntity<List<CargaCombustible>> listar(@RequestParam(value = "moto", required = false) Long idMoto) {
        return ResponseEntity.ok(idMoto != null ? repo.findByIdMoto(idMoto) : repo.findAll());
    }

    @GetMapping("/moto/{idMoto}")
    public ResponseEntity<List<CargaCombustible>> porMoto(@PathVariable Long idMoto) {
        return ResponseEntity.ok(repo.findByIdMoto(idMoto));
    }

    @GetMapping("/precios")
    public ResponseEntity<Map<String, Double>> precios() {
        Map<String, Double> defaults = preciosDefault();
        Map<String, Double> out = new HashMap<>(defaults);
        for (String tipo : defaults.keySet()) {
            configRepo.findById(keyPrecio(tipo)).ifPresent(cfg -> {
                try { out.put(tipo, Double.parseDouble(cfg.getValue())); } catch (Exception ignored) {}
            });
        }
        return ResponseEntity.ok(out);
    }

    @PutMapping("/precios/{tipo}")
    public ResponseEntity<?> actualizarPrecio(@PathVariable String tipo, @RequestBody Map<String, Object> body) {
        String normalized = tipo.toLowerCase();
        if (!preciosDefault().containsKey(normalized)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tipo de combustible no valido"));
        }
        Object raw = body.get("precio");
        if (raw == null) return ResponseEntity.badRequest().body(Map.of("error", "Falta precio"));
        double precio;
        try {
            precio = raw instanceof Number n ? n.doubleValue() : Double.parseDouble(raw.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Precio invalido"));
        }
        if (precio <= 0 || precio > 20) {
            return ResponseEntity.badRequest().body(Map.of("error", "Precio fuera de rango"));
        }
        AppConfig cfg = new AppConfig(keyPrecio(normalized), String.format(java.util.Locale.US, "%.2f", precio));
        configRepo.save(cfg);
        return ResponseEntity.ok(Map.of("tipo", normalized, "precio", precio));
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

    private Map<String, Double> preciosDefault() {
        return Map.of("extra", 2.72, "super", 3.30, "eco", 2.72, "diesel", 1.03);
    }

    private String keyPrecio(String tipo) {
        return "combustible.precio." + tipo.toLowerCase();
    }
}
