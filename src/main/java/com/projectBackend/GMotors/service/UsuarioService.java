package com.projectBackend.GMotors.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.projectBackend.GMotors.model.Rol;
import com.projectBackend.GMotors.model.Usuario;
import com.projectBackend.GMotors.model.UsuarioRol;
import com.projectBackend.GMotors.repository.RolRepository;
import com.projectBackend.GMotors.repository.UsuarioRepository;
import com.projectBackend.GMotors.repository.UsuarioRolRepository;


@Service
public class UsuarioService {

	@Autowired
	private UsuarioRepository usuarioRepository;
	
	
	@Autowired
    private PasswordEncoder passwordEncoder;
	
	@Autowired
	private UsuarioRolRepository usuarioRolRepository;

	@Autowired
	private RolRepository rolRepository;

	@Autowired
	private SupabaseStorageService supabaseStorageService;

	// METODOS

	// Crear Usuario
	public Usuario crearUsuario(Usuario usuario) {

        // Verificar que el correo no esté ya registrado
        if (usuarioRepository.findByCorreo(usuario.getCorreo()).isPresent()) {
            throw new RuntimeException("Este correo ya está registrado. Inicia sesión o usa '¿Olvidaste tu contraseña?'.");
        }

        // Encriptar la contraseña antes de guardar
        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));

        // Guardar en BD
        Usuario nuevo = usuarioRepository.save(usuario);
        
        // Asignar rol CLIENTE por defecto al registrarse.
        // El admin asigna roles superiores (ADMIN/MECANICO) desde la interfaz de Perfiles.
        try {
            // Buscar por nombre (case insensitive) — soporta "CLIENTE", "Cliente", etc.
            Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                .orElseGet(() -> rolRepository.findByNombre("Cliente")
                .orElseGet(() -> rolRepository.findById(2L)
                .orElseThrow(() -> new RuntimeException(
                    "[GMotors] Rol CLIENTE no encontrado en la BD. " +
                    "Ejecutar insertar_admin.sql o reiniciar el backend para que el seeder cree los roles."))));
            
            UsuarioRol usuarioRol = new UsuarioRol(nuevo.getId_usuario(), rolCliente.getId_rol().intValue());
            usuarioRolRepository.save(usuarioRol);
            System.out.println("[GMotors] Rol CLIENTE asignado al usuario: " + nuevo.getCorreo());
        } catch (Exception e) {
            // Log detallado — no silenciar el error
            System.err.println("[GMotors] ERROR: No se pudo asignar rol CLIENTE al usuario " + 
                nuevo.getCorreo() + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        // No enviar la contraseña al frontend
        nuevo.setContrasena(null);

        return nuevo;
	}

	// Modificar Usuario
    public Usuario actualizarUsuario(Long id, Usuario usuarioActualizado) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario con ID " + id + " no encontrado"));

        if (usuarioActualizado.getNombre_completo() != null) {
            usuarioExistente.setNombre_completo(usuarioActualizado.getNombre_completo());
        }
        if (usuarioActualizado.getNombre_usuario() != null) {
            usuarioExistente.setNombre_usuario(usuarioActualizado.getNombre_usuario());
        }
        if (usuarioActualizado.getCorreo() != null) {
            usuarioExistente.setCorreo(usuarioActualizado.getCorreo());
        }
        if (usuarioActualizado.getPais() != null) {
            usuarioExistente.setPais(usuarioActualizado.getPais());
        }
        if (usuarioActualizado.getCiudad() != null) {
            usuarioExistente.setCiudad(usuarioActualizado.getCiudad());
        }
        if (usuarioActualizado.getDescripcion() != null) {
            usuarioExistente.setDescripcion(usuarioActualizado.getDescripcion());
        }
    	// ✅ Manejar cambios de imagen
		if (usuarioActualizado.getRutaimagen() != null) {
			// Si la imagen cambió, eliminar la anterior
			if (usuarioExistente.getRutaimagen() != null &&
				!usuarioExistente.getRutaimagen().equals(usuarioActualizado.getRutaimagen())) {
				
				try {
					supabaseStorageService.eliminarImagen(usuarioExistente.getRutaimagen());
				} catch (Exception e) {
					System.err.println("Advertencia: No se pudo eliminar imagen anterior: " + e.getMessage());
					// No interrumpir el flujo si falla la eliminación
				}
			}
			usuarioExistente.setRutaimagen(usuarioActualizado.getRutaimagen());
		}
     // Si envían nueva contraseña → encriptar
        if (usuarioActualizado.getContrasena() != null && !usuarioActualizado.getContrasena().isBlank()) {
            usuarioExistente.setContrasena(
                passwordEncoder.encode(usuarioActualizado.getContrasena())
            );
        }

        Usuario actualizado = usuarioRepository.save(usuarioExistente);
        actualizado.setContrasena(null);

        return actualizado;
	}

	
	// Método para buscar por ID
	public Optional<Usuario> buscarPorId(Long id) {
		return usuarioRepository.findById(id);
	}

	// Método para listar todos
	public List<Usuario> listarTodos() {
		return usuarioRepository.findAll();
	}

	// En UsuarioService.java
	public void eliminarPorId(Long id) {
		if (!usuarioRepository.existsById(id)) {
			throw new RuntimeException("Usuario con ID " + id + " no encontrado");
		}
		usuarioRepository.deleteById(id);
	}
      
	// Metodo para verificar usuario
    public Usuario login(String correo, String contrasena) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Correo no registrado"));

        // Comparar hash
        if (!passwordEncoder.matches(contrasena, usuario.getContrasena())) {
            throw new RuntimeException("Contraseña incorrecta");
        }
        
        List<UsuarioRol> usuarioRoles = usuarioRolRepository.findByIdUsuario(usuario.getId_usuario());
        usuario.setRoles(usuarioRoles);
        
        // No enviar hash al frontend
        usuario.setContrasena(null);

        return usuario;
    }
    
    public Usuario findByCorreo(String email) {
        return usuarioRepository.findByCorreo(email)
                .orElse(null);
    }
    
    
    public List<UsuarioRol> obtenerRolesUsuario(Long idUsuario) {
        // Verificar que el usuario existe
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new RuntimeException("Usuario con ID " + idUsuario + " no encontrado");
        }
        
        // Retornar todos los roles del usuario
        return usuarioRolRepository.findByIdUsuario(idUsuario);
    }
}
