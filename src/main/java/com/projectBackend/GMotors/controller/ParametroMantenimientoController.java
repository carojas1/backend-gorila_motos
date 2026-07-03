package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.ParametroMantenimiento;
import com.projectBackend.GMotors.repository.ParametroMantenimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/mantenimiento-parametros")
public class ParametroMantenimientoController {

    @Autowired
    private ParametroMantenimientoRepository repo;

    @GetMapping
    public ResponseEntity<List<ParametroMantenimiento>> listar() {
        return ResponseEntity.ok(repo.findAll());
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> actualizarIntervalo(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body
    ) {
        if (!body.containsKey("intervaloKm")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Falta intervaloKm"));
        }
        
        Optional<ParametroMantenimiento> opt = repo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ParametroMantenimiento param = opt.get();
        param.setIntervaloKm(body.get("intervaloKm"));
        repo.save(param);
        
        return ResponseEntity.ok(param);
    }
}
