package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.DetalleFactura;
import com.projectBackend.GMotors.service.DetalleFacturaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/detalles-factura")
public class DetalleFacturaController {

    private final DetalleFacturaService detalleFacturaService;

    public DetalleFacturaController(DetalleFacturaService detalleFacturaService) {
        this.detalleFacturaService = detalleFacturaService;
    }

    // ================= CREAR DETALLE =================
    // LOGICA MANEJADA EN REGISTROCONTROLLER

    // ================= OBTENER DETALLES POR FACTURA =================
    //Logica descontinuad, si se necesita se debe de implementar en DetalleFactura service


    // ================= ELIMINAR DETALLES POR FACTURA =================
    //Logica descontinuad, si se necesita se debe de implementar en DetalleFactura service

}
