package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.Factura;
import com.projectBackend.GMotors.service.FacturaService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService facturaService;

    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    // ================= CREAR FACTURA =================
    @PostMapping
    public ResponseEntity<Factura> crearFacturaRaw(@RequestBody Factura factura) {
        return ResponseEntity.ok(facturaService.save(factura));
    }
    
    // ================= OBTENER TODAS =================
    @GetMapping
    public ResponseEntity<List<Factura>> listarTodas() {
        return ResponseEntity.ok(facturaService.listarTodas());
    }

    // ================= OBTENER FACTURA POR ID =================
    @GetMapping("/{id}")
    public ResponseEntity<Factura> obtenerPorId(@PathVariable Long id) {
        return facturaService.obtenerFacturaPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ================= OBTENER FACTURAS POR USUARIO =================
    //Logica descontinuad, si se necesita se debe de implementar en Factura service

    // ================= RECALCULAR COSTO TOTAL =================
    //Logica descontinuad, si se necesita se debe de implementar en Factura service

    // ================= ELIMINAR FACTURA =================
    //Logica descontinuad, si se necesita se debe de implementar en Factura service

}
