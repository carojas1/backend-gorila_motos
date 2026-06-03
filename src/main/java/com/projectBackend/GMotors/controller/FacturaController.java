package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.Factura;
import com.projectBackend.GMotors.service.FacturaService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/facturas")
public class FacturaController {

    private final FacturaService facturaService;

    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    // ================= CREAR FACTURA =================
    // LOGICA MANEJADA EN REGISTROCONTROLLER
    
    // ================= OBTENER FACTURA POR ID =================
	//Logica descontinuad, si se necesita se debe de implementar en Factura service

    // ================= OBTENER FACTURAS POR USUARIO =================
    //Logica descontinuad, si se necesita se debe de implementar en Factura service

    // ================= RECALCULAR COSTO TOTAL =================
    //Logica descontinuad, si se necesita se debe de implementar en Factura service

    // ================= ELIMINAR FACTURA =================
    //Logica descontinuad, si se necesita se debe de implementar en Factura service

}
