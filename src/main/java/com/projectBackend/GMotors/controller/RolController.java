package com.projectBackend.GMotors.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.projectBackend.GMotors.model.Rol;
import com.projectBackend.GMotors.service.RolService;

@RestController
@RequestMapping("/api/rol")
public class RolController {

    @Autowired
    private RolService rolService;

    // Crear rol
    @PostMapping
    public ResponseEntity<Rol> crearRol(@RequestBody Rol rol) {
        Rol nuevoRol = rolService.crearRol(rol);
        return ResponseEntity.ok(nuevoRol);
    }

    // Obtener rol por ID
    @GetMapping("/{id}")
    public ResponseEntity<Rol> obtenerPorId(@PathVariable Long id) {
        return rolService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Listar todos los roles
    @GetMapping
    public ResponseEntity<List<Rol>> listarTodos() {
        return ResponseEntity.ok(rolService.listarTodos());
    }

    // Actualizar rol
    @PutMapping("/{id}")
    public ResponseEntity<Rol> actualizarRol(@PathVariable Long id, @RequestBody Rol rol) {
        return ResponseEntity.ok(rolService.actualizarRol(id, rol));
    }

    // Eliminar rol
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarRol(@PathVariable Long id) {
        rolService.eliminarRol(id);
        return ResponseEntity.noContent().build();
    }
}

