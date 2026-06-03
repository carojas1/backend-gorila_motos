package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.Tipo;
import com.projectBackend.GMotors.service.TipoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos")
public class TipoController {

    @Autowired
    private TipoService tipoService;

    // Crear tipo
    @PostMapping
    public ResponseEntity<Tipo> crearTipo(@RequestBody Tipo tipo) {
        return ResponseEntity.ok(tipoService.crearTipo(tipo));
    }

    // Obtener todos los tipos
    @GetMapping
    public ResponseEntity<List<Tipo>> obtenerTodos() {
        return ResponseEntity.ok(tipoService.obtenerTodos());
    }

    // Obtener tipo por ID
    @GetMapping("/{id}")
    public ResponseEntity<Tipo> obtenerPorId(@PathVariable Long id) {
        return tipoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Actualizar tipo
    @PutMapping("/{id}")
    public ResponseEntity<Tipo> actualizarTipo(@PathVariable Long id, @RequestBody Tipo tipoActualizado) {
        try {
            return ResponseEntity.ok(tipoService.actualizarTipo(id, tipoActualizado));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Eliminar tipo
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarTipo(@PathVariable Long id) {
        tipoService.eliminarTipo(id);
        return ResponseEntity.noContent().build();
    }
}
