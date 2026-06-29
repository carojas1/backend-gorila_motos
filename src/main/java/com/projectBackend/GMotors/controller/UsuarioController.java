package com.projectBackend.GMotors.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import com.projectBackend.GMotors.config.JwtUtil;
import com.projectBackend.GMotors.dto.AuthResponse;
import com.projectBackend.GMotors.model.Usuario;
import com.projectBackend.GMotors.model.UsuarioRol;
import com.projectBackend.GMotors.repository.UsuarioRepository;
import com.projectBackend.GMotors.service.UsuarioService;
import com.projectBackend.GMotors.service.SupabaseStorageService;


import java.util.List;
import java.util.Optional;
import java.nio.file.*;


@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @Autowired
    private JwtUtil jwtUtil;

    // ✅ POST /api/usuarios → Crear
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Usuario usuario) {
        try {
            Usuario nuevoUsuario = usuarioService.crearUsuario(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario); // 201
        } catch (RuntimeException e) {
            // 409 Conflict si el correo ya existe (más descriptivo que 500)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // ✅ GET /api/usuarios/{id} → Obtener por ID (incluye roles frescos, sin contraseña)
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerPorId(@PathVariable Long id) {
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorId(id);

        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            // Cargar roles actuales desde la BD (resuelve localStorage con datos obsoletos)
            try {
                List<UsuarioRol> roles = usuarioService.obtenerRolesUsuario(id);
                u.setRoles(roles);
            } catch (Exception ignored) {
                // Si falla la carga de roles, devolver usuario sin roles
            }
            u.setContrasena(null); // Nunca enviar el hash al frontend
            return ResponseEntity.ok(u);
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }

    // ✅ PUT /api/usuarios/{id} → Actualizar
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(
            @PathVariable Long id,
            @RequestBody Usuario usuarioActualizado) {
        
        try {
            Usuario usuarioActualizadoDB = usuarioService.actualizarUsuario(id, usuarioActualizado);
            return ResponseEntity.ok(usuarioActualizadoDB); // 200 + datos actualizados
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            // Devuelve 400 con el mensaje de error para poder depurar
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // ✅ GET /api/usuarios → Listar todos
    @GetMapping
    public ResponseEntity<List<Usuario>> listarTodos() {
        List<Usuario> usuarios = usuarioService.listarTodos();
        return ResponseEntity.ok(usuarios); // 200 + lista (aunque esté vacía)
    }

    // ✅ DELETE /api/usuarios/{id} → Eliminar (opcional, pero útil)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            // Primero verificamos que exista (usando tu método buscarPorId)
            if (usuarioService.buscarPorId(id).isEmpty()) {
                return ResponseEntity.notFound().build(); // 404 si no existe
            }
            usuarioService.eliminarPorId(id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Login - autenticación 
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario usuario) {
        try {
            Usuario usuarioLogueado = usuarioService.login(
                usuario.getCorreo(),
                usuario.getContrasena()
            );

            String token = jwtUtil.generarToken(usuarioLogueado.getCorreo());

            return ResponseEntity.ok(
                new AuthResponse(usuarioLogueado, token)
            );

        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<?> obtenerRolesUsuario(@PathVariable Long id) {
        try {
            List<?> roles = usuarioService.obtenerRolesUsuario(id);
            return ResponseEntity.ok(roles);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
    
    // --------------------- Subir Imagen ---------------------
    
    @PostMapping("/upload")
    public ResponseEntity<?> subirImagen(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            //System.out.println("[UPLOAD] Archivo recibido: " + file.getOriginalFilename());
            //System.out.println("[UPLOAD] Content-Type: " + file.getContentType());
            //System.out.println("[UPLOAD] Tamaño: " + file.getSize());

            // Validar archivo vacío
            if (file.isEmpty()) {
                //System.out.println("[UPLOAD] ERROR: Archivo vacío");
                return ResponseEntity
                        .badRequest()
                        .body(new UploadResponse(null, "El archivo está vacío"));
            }

            // Validar tipo de archivo (acepta image/* y también octet-stream que es lo que envía Flutter)
            String filename = file.getOriginalFilename();
            //System.out.println("[UPLOAD] Validando extensión: " + filename);

            boolean esImagen = filename != null && (
                filename.toLowerCase().endsWith(".jpg") ||
                filename.toLowerCase().endsWith(".jpeg") ||
                filename.toLowerCase().endsWith(".png") ||
                filename.toLowerCase().endsWith(".gif") ||
                filename.toLowerCase().endsWith(".webp")
            );

            if (!esImagen) {
                //System.out.println("[UPLOAD] ERROR: Archivo no es imagen válida: " + filename);
                return ResponseEntity
                        .badRequest()
                        .body(new UploadResponse(null, "El archivo debe ser una imagen (jpg, png, gif, webp)"));
            }

            // Validar tamaño (máximo 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                //System.out.println("[UPLOAD] ERROR: Archivo muy grande: " + file.getSize());
                return ResponseEntity
                        .badRequest()
                        .body(new UploadResponse(null, "El archivo no puede ser mayor a 5MB"));
            }

            //System.out.println("[UPLOAD] Validaciones pasadas, subiendo a Supabase...");

            // Subir a Supabase
            String urlImagen = supabaseStorageService.subirImagenUsuario(file);

            //System.out.println("[UPLOAD] URL recibida de Supabase: " + urlImagen);

            return ResponseEntity.ok(new UploadResponse(urlImagen, "Imagen subida exitosamente"));

        } catch (Exception e) {
            //System.out.println("[UPLOAD] EXCEPCIÓN: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UploadResponse(null, "Error al subir la imagen: " + e.getMessage()));
        }
    }

    // ─── Canjer código de referido: 50 puntos para ambos ───────────────────────
    // POST /api/usuarios/{id}/usar-referido   body: { "codigo": "nombre_usuario_del_referente" }
    // Reglas: solo una vez por usuario; el código debe existir; no puede ser el propio código.
    @PostMapping("/{id}/usar-referido")
    public ResponseEntity<?> usarReferido(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        String codigo = body.get("codigo");
        if (codigo == null || codigo.isBlank()) {
            return ResponseEntity.badRequest().body("Código requerido");
        }
        codigo = codigo.trim().toLowerCase();

        Optional<Usuario> selfOpt = usuarioRepository.findById(id);
        if (selfOpt.isEmpty()) return ResponseEntity.notFound().build();
        Usuario self = selfOpt.get();

        // Ya usó un código antes
        if (self.getCodigoReferido() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Ya usaste un código de referido anteriormente");
        }
        // Buscar referente por nombre_usuario (case-insensitive)
        final String codigoFinal = codigo;
        Optional<Usuario> refOpt = usuarioRepository.findAll().stream()
            .filter(u -> u.getNombre_usuario() != null &&
                         u.getNombre_usuario().trim().toLowerCase().equals(codigoFinal))
            .findFirst();

        if (refOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Código no válido. Verifica que sea el nombre de usuario exacto");
        }
        Usuario referente = refOpt.get();

        // No puede usar su propio código
        if (referente.getId_usuario().equals(id)) {
            return ResponseEntity.badRequest().body("No puedes usar tu propio código");
        }

        // Acreditar 50 puntos a cada uno
        self.setPuntosBonus((self.getPuntosBonus() != null ? self.getPuntosBonus() : 0) + 50);
        self.setCodigoReferido(referente.getNombre_usuario());
        referente.setPuntosBonus((referente.getPuntosBonus() != null ? referente.getPuntosBonus() : 0) + 50);

        usuarioRepository.save(self);
        usuarioRepository.save(referente);

        return ResponseEntity.ok(java.util.Map.of(
            "mensaje", "¡Código canjeado! 50 puntos acreditados a tu cuenta y a la de tu referente",
            "puntos_bonus", self.getPuntosBonus()
        ));
    }

    // ============== INNER CLASS ==============
    
    /**
     * Clase para la respuesta de upload
     */
    public static class UploadResponse {
        public String url;
        public String mensaje;

        public UploadResponse(String url, String mensaje) {
            this.url = url;
            this.mensaje = mensaje;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMensaje() {
            return mensaje;
        }

        public void setMensaje(String mensaje) {
            this.mensaje = mensaje;
        }
    }
}