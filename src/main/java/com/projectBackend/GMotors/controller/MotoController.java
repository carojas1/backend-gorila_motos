package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.config.FlaskOcrClient;
import com.projectBackend.GMotors.model.Moto;
import com.projectBackend.GMotors.service.MotoService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.projectBackend.GMotors.service.SupabaseStorageService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/motos")
public class MotoController {

    @Autowired
    private MotoService motoService;
    
    @Autowired
    private SupabaseStorageService supabaseStorageService;
    
    @Autowired
    private FlaskOcrClient flaskOcrClient;

    // ======================================================
    // CREAR MOTO
    // ======================================================
    @PostMapping
    public ResponseEntity<Moto> crearMoto(@RequestBody Moto moto) {
        try {
            Moto motoCreada = motoService.crearMoto(moto);
            return ResponseEntity.status(HttpStatus.CREATED).body(motoCreada);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // ======================================================
    // BUSCAR MOTO POR ID
    // ======================================================
    @GetMapping("/{id}")
    public ResponseEntity<Moto> obtenerPorId(@PathVariable Long id) {
        Optional<Moto> moto = motoService.buscarPorId(id);
        return moto.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    // ======================================================
    // BUSCAR MOTO POR PLACA
    // ======================================================
    @GetMapping("/placa/{placa}")
    public ResponseEntity<Moto> obtenerPorPlaca(@PathVariable String placa) {

     Optional<Moto> moto = motoService.buscarPorPlaca(placa);

     return moto.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
 }
 
    // ======================================================
    // LISTAR TODAS LAS MOTOS
    // ======================================================
    @GetMapping
    public ResponseEntity<List<Moto>> listarTodas() {
        List<Moto> motos = motoService.listarTodas();
        return ResponseEntity.ok(motos);
    }
    
    // ======================================================
    // LISTAR MOTOS POR USUARIO
    // ======================================================
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Moto>> listarMotosPorUsuario(@PathVariable Long idUsuario) {
        List<Moto> motos = motoService.listarPorUsuario(idUsuario);
        return ResponseEntity.ok(motos);
    }

    // ======================================================
    // ACTUALIZAR MOTO
    // ======================================================
    @PutMapping("/{id}")
    public ResponseEntity<Moto> actualizarMoto(
            @PathVariable Long id, 
            @RequestBody Moto motoActualizada) {
        try {
            Moto moto = motoService.actualizarMoto(id, motoActualizada);
            return ResponseEntity.ok(moto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // ======================================================
    // ELIMINAR MOTO
    // ======================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarMoto(@PathVariable Long id) {
        try {
            motoService.eliminarMoto(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // ======================================================
    // DETECTAR PLACA CON OCR 
    // ======================================================
    
    @PostMapping("/ocr/placa")
    public ResponseEntity<Map<String, String>> detectarPlaca(
            @RequestParam("image") MultipartFile image
    ) {
        try {
            System.out.println("[CONTROLLER] Recibiendo imagen para OCR...");
            
            // Consumir Flask OCR
            String placaDetectada = flaskOcrClient.detectarPlaca(image);

            if (placaDetectada == null || placaDetectada.isBlank()) {
                System.out.println("❌ [CONTROLLER] No se detectó placa");
                return ResponseEntity.ok(Map.of("placa", ""));
            }

            System.out.println("✅ [CONTROLLER] Placa detectada: " + placaDetectada);
            return ResponseEntity.ok(Map.of("placa", placaDetectada));

        } catch (RuntimeException e) {
            // Aquí capturamos si Flask está apagado
            System.err.println("⚠️ [CONTROLLER] Servidor OCR apagado o no disponible: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("placa", "", "error", "Servidor OCR no disponible. Contacte con un administrador."));
        } catch (Exception e) {
            System.err.println("❌ [CONTROLLER] Error en OCR:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("placa", "", "error", "Error al procesar la imagen: " + e.getMessage()));
        }
    }

    
    
 // ======================================================
 // BUSCAR USUARIO POR PLACA (CON OCR)
 // ======================================================
 @PostMapping("/ocr/buscar-dueno")
 public ResponseEntity<?> buscarDuenoPorPlaca(@RequestParam("image") MultipartFile image) {
	    try {
	        // Detectar placa con OCR
	        String placaDetectada = flaskOcrClient.detectarPlaca(image);

	        if (placaDetectada == null || placaDetectada.isBlank()) {
	            return ResponseEntity.ok(Map.of(
	                "success", false,
	                "mensaje", "No se pudo detectar la placa en la imagen"
	            ));
	        }

	        Optional<Moto> motoOpt = motoService.buscarPorPlaca(placaDetectada);

	        if (motoOpt.isEmpty()) {
	            return ResponseEntity.ok(Map.of(
	                "success", false,
	                "placa", placaDetectada,
	                "mensaje", "No se encontró vehículo registrado con esta placa"
	            ));
	        }

	        Moto moto = motoOpt.get();

	        if (moto.getUsuario() == null) {
	            return ResponseEntity.ok(Map.of(
	                "success", false,
	                "placa", placaDetectada,
	                "mensaje", "El vehículo no tiene un dueño registrado"
	            ));
	        }

	        return ResponseEntity.ok(Map.of(
	            "success", true,
	            "placa", placaDetectada,
	            "idUsuario", moto.getId_usuario(),
	            "nombreCompleto", moto.getUsuario().getNombre_completo(),
	            "idMoto", moto.getIdMoto(),
	            "modelo", moto.getModelo(),
	            "marca", moto.getMarca()
	        ));

	    } catch (RuntimeException e) {
	        System.err.println("⚠️ [CONTROLLER] Servidor OCR apagado o no disponible: " + e.getMessage());
	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(Map.of("success", false, "mensaje", "Servidor OCR no disponible. Contacte con un administrador."));
	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(Map.of("success", false, "mensaje", "Error al procesar la solicitud: " + e.getMessage()));
	    }
	}

 
 
    // ======================================================
    // ACTUALIZAR PLACA CON OCR (FLASK)
    // ======================================================
    
 @PostMapping("/{id}/ocr-placa")
 public ResponseEntity<Moto> actualizarPlacaConOCR(
         @PathVariable Long id,
         @RequestParam("image") MultipartFile image
 ) {
     try {
         String placaDetectada = flaskOcrClient.detectarPlaca(image);

         if (placaDetectada == null || placaDetectada.isBlank()) {
             return ResponseEntity.badRequest().build();
         }

         Moto patch = new Moto();
         patch.setPlaca(placaDetectada);

         Moto motoActualizada = motoService.actualizarMoto(id, patch);

         return ResponseEntity.ok(motoActualizada);

     } catch (RuntimeException e) {
         System.err.println("⚠️ Servidor OCR apagado o no disponible: " + e.getMessage());
         return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
     } catch (Exception e) {
         e.printStackTrace();
         return ResponseEntity.internalServerError().build();
     }
 }

    // ======================================================
    // SUBIR FOTO MOTO
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
            String urlImagen = supabaseStorageService.subirImagenMoto(file);

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

    
