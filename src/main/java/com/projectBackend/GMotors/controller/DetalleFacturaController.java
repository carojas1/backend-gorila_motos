package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.dto.DetalleFacturaDTO;
import com.projectBackend.GMotors.service.FacturaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/detalles-factura")
public class DetalleFacturaController {

    private final FacturaService facturaService;

    public DetalleFacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    // ================= CREAR DETALLE FACTURA =================
    @PostMapping
    public ResponseEntity<com.projectBackend.GMotors.model.DetalleFactura> crearDetalle(@RequestBody com.projectBackend.GMotors.model.DetalleFactura detalle) {
        return ResponseEntity.ok(facturaService.saveDetalle(detalle));
    }

    // ================= OBTENER DETALLES POR FACTURA =================
    // GET /api/detalles-factura/factura/{idFactura}
    // Devuelve mano de obra + repuestos (inventario y manual) de una factura.
    // Lo consumen: comprobante (InvoicePage), ranking de clientes y el Excel de contabilidad.
    @GetMapping("/factura/{idFactura}")
    public ResponseEntity<List<DetalleFacturaDTO>> porFactura(@PathVariable Long idFactura) {
        return ResponseEntity.ok(facturaService.obtenerDetallesPorFactura(idFactura));
    }
}
