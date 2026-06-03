package com.projectBackend.GMotors.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.projectBackend.GMotors.model.Usuario;
import com.projectBackend.GMotors.model.Moto;
import com.projectBackend.GMotors.model.UsuarioRol;
import com.projectBackend.GMotors.service.UsuarioService;
import com.projectBackend.GMotors.service.MotoService;
import com.projectBackend.GMotors.dto.QuickAccountDTO;
import com.projectBackend.GMotors.repository.UsuarioRepository;
import com.projectBackend.GMotors.repository.UsuarioRolRepository;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/quick-accounts")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.GET})
public class QuickAccountController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private MotoService motoService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioRolRepository usuarioRolRepository;

    // ========================
    // CREAR CUENTA RÁPIDA
    // ========================
    @PostMapping("/create")
    public ResponseEntity<?> createQuickAccount(@RequestBody QuickAccountDTO quickAccountDTO) {
        try {
            // ========================
            // VALIDAR DATOS REQUERIDOS
            // ========================
            if (quickAccountDTO.getNombre_completo() == null || quickAccountDTO.getNombre_completo().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio"));
            }
            if (quickAccountDTO.getPlaca() == null || quickAccountDTO.getPlaca().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La placa es obligatoria"));
            }

            // ========================
            // GENERAR NOMBRE DE USUARIO
            // ========================
            String nombreUsuario = generarNombreUsuario(quickAccountDTO.getNombre_completo());

            // ========================
            // GENERAR EMAIL Y CONTRASEÑA
            // ========================
            String defaultEmail = nombreUsuario + "@gmotors.com";
            String defaultPassword = "root111";
            String passwordEncriptada = passwordEncoder.encode(defaultPassword);

            // ========================
            // CREAR USUARIO CON DATOS POR DEFECTO
            // ========================
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombre_completo(quickAccountDTO.getNombre_completo());
            nuevoUsuario.setNombre_usuario(nombreUsuario);
            nuevoUsuario.setCorreo(defaultEmail);
            nuevoUsuario.setContrasena(passwordEncriptada);
            nuevoUsuario.setPais("Ecuador");
            nuevoUsuario.setCiudad("Cuenca");
            nuevoUsuario.setDescripcion("Cuenta rápida");
            nuevoUsuario.setRutaimagen("null");

            // GUARDAR USUARIO DIRECTAMENTE EN EL REPOSITORIO
            Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

            // ========================
            // ASIGNAR ROL 2 (CLIENTE) AL USUARIO
            // ========================
            UsuarioRol usuarioRol = new UsuarioRol();
            usuarioRol.setIdUsuario(usuarioGuardado.getId_usuario());
            usuarioRol.setIdRol(2); // id_rol = 2 (Cliente)
            usuarioRol.setEstado(1); // Estado activo
            usuarioRolRepository.save(usuarioRol);

            // ========================
            // CREAR MOTO ASOCIADA
            // ========================
            Moto nuevaMoto = new Moto();
            nuevaMoto.setPlaca(quickAccountDTO.getPlaca().toUpperCase());
            nuevaMoto.setAnio(2024);
            nuevaMoto.setMarca("Por especificar");
            nuevaMoto.setModelo("Por especificar");
            nuevaMoto.setNombreMoto(quickAccountDTO.getNombre_completo());
            nuevaMoto.setTipoMoto("Por especificar");
            nuevaMoto.setKilometraje(0);
            nuevaMoto.setCilindraje(0);
            nuevaMoto.setId_usuario(usuarioGuardado.getId_usuario());
            nuevaMoto.setRuta_imagenMotos("default");

            // Guardar moto
            Moto motoGuardada = motoService.crearMoto(nuevaMoto);

            // ========================
            // RESPUESTA EXITOSA
            // ========================
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("usuarioId", usuarioGuardado.getId_usuario());
            response.put("nombre", usuarioGuardado.getNombre_completo());
            response.put("email", defaultEmail);
            response.put("nombreUsuario", usuarioGuardado.getNombre_usuario());
            response.put("placa", motoGuardada.getPlaca());
            response.put("contrasena", defaultPassword);
            response.put("mensaje", "Cuenta rápida creada exitosamente");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ========================
    // Generar nombre_usuario a partir del nombre_completo
    // ========================
    private String generarNombreUsuario(String nombreCompleto) {
        // Convertir a minúsculas y trim
        String usuario = nombreCompleto.toLowerCase().trim();

        // Reemplazar espacios con puntos
        usuario = usuario.replaceAll("\\s+", ".");

        // Remover acentos
        usuario = usuario.replaceAll("[áàäâ]", "a");
        usuario = usuario.replaceAll("[éèëê]", "e");
        usuario = usuario.replaceAll("[íìïî]", "i");
        usuario = usuario.replaceAll("[óòöô]", "o");
        usuario = usuario.replaceAll("[úùüû]", "u");
        usuario = usuario.replaceAll("[ñ]", "n");

        // Remover caracteres no alfanuméricos excepto puntos
        usuario = usuario.replaceAll("[^a-z0-9.]", "");

        // Limitar a 20 caracteres
        if (usuario.length() > 20) {
            usuario = usuario.substring(0, 20);
        }

        return usuario;
    }
}