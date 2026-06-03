package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.Ruta;
import com.projectBackend.GMotors.service.RutaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rutas")
public class RutaController {

    @Autowired
    private RutaService rutaService;

    // Crear ruta
    @PostMapping
    public ResponseEntity<Ruta> crearRuta(@RequestBody Ruta ruta) {
        try {
            Ruta nuevaRuta = rutaService.crearRuta(ruta);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaRuta);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // Listar rutas del usuario
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Ruta>> listarRutasPorUsuario(@PathVariable Long idUsuario) {
        List<Ruta> rutas = rutaService.listarRutasPorUsuario(idUsuario);
        return ResponseEntity.ok(rutas);
    }

    // Obtener ruta por ID
    @GetMapping("/{id}")
    public ResponseEntity<Ruta> obtenerRutaPorId(@PathVariable Long id) {
        Optional<Ruta> ruta = rutaService.obtenerRutaPorId(id);
        return ruta.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Actualizar ruta
    @PutMapping("/{id}")
    public ResponseEntity<Ruta> actualizarRuta(
            @PathVariable Long id,
            @RequestBody Ruta rutaActualizada) {
        try {
            Ruta ruta = rutaService.actualizarRuta(id, rutaActualizada);
            return ResponseEntity.ok(ruta);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Eliminar ruta
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarRuta(@PathVariable Long id) {
        try {
            rutaService.eliminarRuta(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}