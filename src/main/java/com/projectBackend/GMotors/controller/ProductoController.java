package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.Producto;
import com.projectBackend.GMotors.service.ProductoService;
import com.projectBackend.GMotors.service.ResendEmailService;
import com.projectBackend.GMotors.service.SupabaseStorageService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ResendEmailService resendEmailService;

    @Autowired
    private SupabaseStorageService supabaseStorageService;



    // Obtener todos los productos
    @GetMapping
    public ResponseEntity<List<Producto>> getAllProductos() {
        return ResponseEntity.ok(productoService.getAllProductos());
    }

    // Obtener por ID
    @GetMapping("/{id}")
    public ResponseEntity<Producto> getProductoById(@PathVariable Long id) {
        Producto p = productoService.getProductoById(id);
        if (p != null) return ResponseEntity.ok(p);
        return ResponseEntity.notFound().build();
    }

    // Crear
    @PostMapping
    public ResponseEntity<Producto> createProducto(@RequestBody Producto producto) {
        return ResponseEntity.ok(productoService.createProducto(producto));
    }

    // Actualizar
    @PutMapping("/{id}")
    public ResponseEntity<Producto> updateProducto(
            @PathVariable Long id,
            @RequestBody Producto producto
    ) {
        Producto updated = productoService.updateProducto(id, producto);
        if (updated != null) return ResponseEntity.ok(updated);
        return ResponseEntity.notFound().build();
    }
    

    // Eliminar
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProducto(@PathVariable Long id) {
        if (productoService.deleteProducto(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    /* ── Envío de comprobante de venta de inventario por email ── */
    @PostMapping("/venta-comprobante")
    public ResponseEntity<Map<String, Object>> enviarComprobanteVenta(
            @RequestBody Map<String, Object> datos) {
        String correo          = (String) datos.get("correo");
        String nombreCliente   = (String) datos.get("nombreCliente");
        String nombreProducto  = (String) datos.get("nombreProducto");
        String codigoProducto  = datos.get("codigoProducto") != null ? (String) datos.get("codigoProducto") : null;
        int    cantidad        = ((Number) datos.get("cantidad")).intValue();
        double pvp             = ((Number) datos.get("pvp")).doubleValue();
        double total           = ((Number) datos.get("total")).doubleValue();
        String fecha           = (String) datos.get("fecha");
        String referencia      = datos.get("referencia") != null ? (String) datos.get("referencia") : null;

        boolean sent = resendEmailService.enviarComprobanteInventario(
                correo, nombreCliente, nombreProducto, codigoProducto, cantidad, pvp, total, fecha, referencia);

        Map<String, Object> response = new HashMap<>();
        response.put("sent", sent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/eliminar")
    public ResponseEntity<Map<String, String>> eliminarProductos(@RequestBody List<Long> ids) {
        productoService.deleteProductos(ids);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Productos eliminados correctamente");

        return ResponseEntity.ok(response);
    }
    
    
 // ======================================================
    // SUBIR PRODUCTOS FOTO
    // ======================================================
    @PostMapping("/upload")
    public ResponseEntity<?> subirImagen(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            //System.out.println("[MOTO-UPLOAD] Archivo recibido: " + file.getOriginalFilename());
            //System.out.println("[MOTO-UPLOAD] Content-Type: " + file.getContentType());
            //System.out.println("[MOTO-UPLOAD] Tamaño: " + file.getSize());

            // Validar archivo vacío
            if (file.isEmpty()) {
                //System.out.println("[MOTO-UPLOAD] ERROR: Archivo vacío");
                return ResponseEntity
                        .badRequest()
                        .body(new UsuarioController.UploadResponse(null, "El archivo está vacío"));
            }

            // Validar tipo de archivo por extensión
            String filename = file.getOriginalFilename();
            //System.out.println("[MOTO-UPLOAD] Validando extensión: " + filename);

            boolean esImagen = filename != null && (
                filename.toLowerCase().endsWith(".jpg") ||
                filename.toLowerCase().endsWith(".jpeg") ||
                filename.toLowerCase().endsWith(".png") ||
                filename.toLowerCase().endsWith(".gif") ||
                filename.toLowerCase().endsWith(".webp")
            );

            if (!esImagen) {
                //System.out.println("[MOTO-UPLOAD] ERROR: Archivo no es imagen válida: " + filename);
                return ResponseEntity
                        .badRequest()
                        .body(new UsuarioController.UploadResponse(null, "El archivo debe ser una imagen (jpg, png, gif, webp)"));
            }

            // Validar tamaño (máximo 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                //System.out.println("[MOTO-UPLOAD] ERROR: Archivo muy grande: " + file.getSize());
                return ResponseEntity
                        .badRequest()
                        .body(new UsuarioController.UploadResponse(null, "El archivo no puede ser mayor a 5MB"));
            }

            //System.out.println("[MOTO-UPLOAD] Validaciones pasadas, subiendo a Supabase...");

            // Subir a Supabase (carpeta motos/perfil/)
            String urlImagen = supabaseStorageService.subirImagenProducto(file);

            //System.out.println("[MOTO-UPLOAD] URL recibida de Supabase: " + urlImagen);

            return ResponseEntity.ok(new UsuarioController.UploadResponse(urlImagen, "Imagen subida exitosamente"));

        } catch (Exception e) {
            System.out.println("[MOTO-UPLOAD] EXCEPCIÓN: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UsuarioController.UploadResponse(null, "Error al subir la imagen: " + e.getMessage()));
        }
    }
    
    @PostMapping("/venta-comprobante")
    public ResponseEntity<?> enviarComprobante(@RequestBody Map<String, Object> payload) {
        try {
            String correo = (String) payload.get("correo");
            String nombreCliente = (String) payload.get("nombreCliente");
            String nombreProducto = (String) payload.get("nombreProducto");
            String codigoProducto = (String) payload.get("codigoProducto");
            Integer cantidad = (Integer) payload.get("cantidad");
            Double pvp = ((Number) payload.get("pvp")).doubleValue();
            Double total = ((Number) payload.get("total")).doubleValue();
            String fecha = (String) payload.get("fecha");
            String referencia = (String) payload.get("referencia");

            String html = "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
               "<style>" +
               "body{margin:0;padding:0;background:#f5f6fa;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#333}" +
               ".wrap{max-width:600px;margin:30px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 15px rgba(0,0,0,0.05)}" +
               ".header{background:#E11428;padding:25px 40px;text-align:center;color:#fff}" +
               ".content{padding:40px}" +
               ".btn{display:inline-block;background:#E11428;color:#fff;text-decoration:none;padding:12px 28px;border-radius:6px;font-weight:600;font-size:14px;letter-spacing:0.5px}" +
               "</style></head><body>" +
               "<div class='wrap'>" +
               "<div class='header'>" +
               "<h1 style='margin:0;font-size:24px;font-weight:800;letter-spacing:1px'>GORILA MOTOS</h1>" +
               "<p style='margin:5px 0 0;font-size:12px;opacity:0.9;text-transform:uppercase'>Comprobante de Venta</p>" +
               "</div>" +
               "<div class='content'>" +
               "<h2 style='margin:0 0 20px;font-size:20px;font-weight:700;color:#222'>Hola " + nombreCliente + ",</h2>" +
               "<p style='color:#555;font-size:15px;line-height:1.6;margin:0 0 25px'>" +
               "Gracias por tu compra. Aquí tienes el detalle de los productos adquiridos:</p>" +
               "<div style='background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;padding:20px;margin-bottom:30px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse'>" +
               "<tr><td style='padding:8px 0;color:#6b7280;font-size:13px'>Referencia</td><td style='padding:8px 0;text-align:right;font-weight:600'>" + (referencia != null ? referencia : "Venta Directa") + "</td></tr>" +
               "<tr><td style='padding:8px 0;color:#6b7280;font-size:13px'>Fecha</td><td style='padding:8px 0;text-align:right;font-weight:600'>" + fecha + "</td></tr>" +
               "<tr><td style='padding:8px 0;color:#6b7280;font-size:13px;border-top:1px solid #e5e7eb'>Producto</td><td style='padding:8px 0;text-align:right;font-weight:600;border-top:1px solid #e5e7eb'>" + nombreProducto + " (" + codigoProducto + ")</td></tr>" +
               "<tr><td style='padding:8px 0;color:#6b7280;font-size:13px'>Cantidad</td><td style='padding:8px 0;text-align:right;font-weight:600'>" + cantidad + "</td></tr>" +
               "<tr><td style='padding:8px 0;color:#6b7280;font-size:13px'>Precio Unit.</td><td style='padding:8px 0;text-align:right;font-weight:600'>$" + String.format("%.2f", pvp) + "</td></tr>" +
               "<tr><td style='padding:12px 0 0;color:#111;font-size:16px;font-weight:700;border-top:1px solid #e5e7eb'>Total</td><td style='padding:12px 0 0;text-align:right;color:#E11428;font-size:18px;font-weight:800;border-top:1px solid #e5e7eb'>$" + String.format("%.2f", total) + "</td></tr>" +
               "</table></div>" +
               "<div style='text-align:center;margin:30px 0 10px'>" +
               "<a href='https://pagina-web-gorila-motos.vercel.app/' class='btn'>Visitar Tienda en Línea</a>" +
               "</div>" +
               "</div>" +
               "<div style='background:#f3f4f6;padding:20px;text-align:center;color:#6b7280;font-size:12px'>" +
               "© " + java.time.Year.now().getValue() + " Gorila Motos. Todos los derechos reservados." +
               "</div></div></body></html>";

            resendEmailService.enviar(correo, "Comprobante de compra - Gorila Motos", html);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error enviando email");
        }
    }
}
