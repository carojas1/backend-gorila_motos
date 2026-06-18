package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.PagoEmpleado;
import com.projectBackend.GMotors.service.PagoEmpleadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pagos-empleado")
public class PagoEmpleadoController {

    @Autowired
    private PagoEmpleadoService service;

    /** Todos los gastos (empleados + generales) — para admin contabilidad */
    @GetMapping
    public ResponseEntity<List<PagoEmpleado>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    /** Pagos de un empleado específico */
    @GetMapping("/{idEmpleado}")
    public ResponseEntity<List<PagoEmpleado>> listarPorEmpleado(@PathVariable Long idEmpleado) {
        return ResponseEntity.ok(service.listarPorEmpleado(idEmpleado));
    }

    @PostMapping
    public ResponseEntity<PagoEmpleado> crear(@RequestBody PagoEmpleado pago) {
        return ResponseEntity.ok(service.crear(pago));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
