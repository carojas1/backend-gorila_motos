package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.model.Producto;
import com.projectBackend.GMotors.service.ProductoService;
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
}
