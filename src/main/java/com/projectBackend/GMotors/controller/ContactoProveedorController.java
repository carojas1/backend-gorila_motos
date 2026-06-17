package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.ContactoProveedor;
import com.projectBackend.GMotors.repository.ContactoProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contactos de proveedores — compartido en la nube.
 */
@RestController
@RequestMapping("/api/proveedores-contactos")
public class ContactoProveedorController {

    @Autowired
    private ContactoProveedorRepository repo;

    /** Lista todos los contactos de proveedor. */
    @GetMapping
    public ResponseEntity<List<ContactoProveedor>> listar() {
        return ResponseEntity.ok(repo.findAll());
    }

    /** Crea o actualiza el contacto de un proveedor (por código). */
    @PutMapping("/{codigo}")
    public ResponseEntity<?> guardar(@PathVariable String codigo, @RequestBody ContactoProveedor body) {
        if (codigo == null || codigo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código requerido"));
        }
        body.setCodigo(codigo);
        return ResponseEntity.ok(repo.save(body));
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<?> borrar(@PathVariable String codigo) {
        if (repo.existsById(codigo)) repo.deleteById(codigo);
        return ResponseEntity.ok(Map.of("mensaje", "Contacto eliminado"));
    }
}
