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
        if (usuarioActualizado.getTelefono() != null) {
            usuarioExistente.setTelefono(usuarioActualizado.getTelefono());
        }
        if (usuarioActualizado.getCedula() != null) {
            usuarioExistente.setCedula(usuarioActualizado.getCedula());
        }
        if (usuarioActualizado.getDireccion() != null) {
            usuarioExistente.setDireccion(usuarioActualizado.getDireccion());
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

        // Comparar hash (o permitir si es login de Google verificado por el frontend)
        if (!passwordEncoder.matches(contrasena, usuario.getContrasena()) && !contrasena.startsWith("gm_google_")) {
            throw new RuntimeException("Contraseña incorrecta");
        }
        
        List<UsuarioRol> usuarioRoles = usuarioRolRepository.findByIdUsuarioAndEstado(usuario.getId_usuario(), 1);
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
        
        // Retornar solo los roles ACTIVOS del usuario (estado = 1)
        return usuarioRolRepository.findByIdUsuarioAndEstado(idUsuario, 1);
    }
}
