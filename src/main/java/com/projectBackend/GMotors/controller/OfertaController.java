package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.service.OfertaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ofertas")
@CrossOrigin(origins = "*")
public class OfertaController {

    @Autowired
    private OfertaService ofertaService;

    /**
     * POST /api/ofertas/enviar
     * Body: { asunto, mensaje, roles: [2] }
     * roles: 1=ADMIN, 2=CLIENTE, 3=MECANICO (pueden combinarse)
     * Solo accesible para ADMIN (el frontend ya lo restringe).
     */
    @PostMapping("/enviar")
    public ResponseEntity<Map<String, Object>> enviar(@RequestBody Map<String, Object> req) {
        String asunto  = (String) req.get("asunto");
        String mensaje = (String) req.get("mensaje");

        @SuppressWarnings("unchecked")
        List<Integer> roles = req.get("roles") instanceof List<?>
            ? ((List<?>) req.get("roles")).stream()
                  .filter(o -> o instanceof Number)
                  .map(o -> ((Number) o).intValue())
                  .toList()
            : List.of(2);

        if (asunto == null || asunto.isBlank() || mensaje == null || mensaje.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "exito", false, "mensaje", "Asunto y mensaje son obligatorios"
            ));
        }

        OfertaService.OfertaResultado resultado = ofertaService.enviarOferta(asunto, mensaje, roles);

        return ResponseEntity.ok(Map.of(
            "exito",    true,
            "total",    resultado.total(),
            "enviados", resultado.enviados(),
            "errores",  resultado.errores(),
            "mensaje",  resultado.enviados() + " de " + resultado.total() + " correos enviados"
        ));
    }
}
