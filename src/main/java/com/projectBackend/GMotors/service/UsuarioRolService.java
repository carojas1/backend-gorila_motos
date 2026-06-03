package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.dto.UsuarioRolDTO;
import com.projectBackend.GMotors.model.UsuarioRol;
import com.projectBackend.GMotors.model.UsuarioRolId;
import com.projectBackend.GMotors.repository.UsuarioRolRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioRolService {

	@Autowired
	private UsuarioRolRepository usuarioRolRepository;

	@Autowired
	private RoleValidationService validationService; // ← NUEVO

	// ═══════════════════════════════════════════════════════
	// OPERACIONES PRINCIPALES (con validaciones)
	// ═══════════════════════════════════════════════════════

	/**
	 * ASIGNAR ROL - Agregar rol adicional a usuario Con validaciones de seguridad y
	 * combinación de roles
	 */
	@Transactional
	public UsuarioRol asignarRol(Long usuarioId, Integer rolId, Long adminId) {

		// 1. VALIDAR QUE QUIEN ASIGNA ES ADMIN
		validationService.validarEsAdmin(adminId);

		// 2. NO PUEDE MODIFICAR SUS PROPIOS ROLES
		if (usuarioId.equals(adminId)) {
			throw new IllegalArgumentException("No puedes modificar tus propios roles");
		}

		// 3. VALIDAR COMBINACIÓN DE ROLES
		validationService.validarCombinacionRoles(usuarioId, rolId);

		// 4. VERIFICAR QUE NO EXISTA (activo o inactivo)
		UsuarioRolId pk = new UsuarioRolId(usuarioId, rolId);
		Optional<UsuarioRol> existente = usuarioRolRepository.findById(pk);

		if (existente.isPresent()) {
			UsuarioRol relacion = existente.get();

			if (relacion.getEstado() == 1) {
				throw new IllegalArgumentException("El usuario ya tiene este rol activo");
			}

			// Si existe pero está inactivo, reactivar
			relacion.setEstado(1);
			relacion.setFechaModificacion(LocalDateTime.now());
			return usuarioRolRepository.save(relacion);
		}

		// 5. CREAR NUEVO ROL
		UsuarioRol nuevo = new UsuarioRol(usuarioId, rolId);
		nuevo.setEstado(1);
		nuevo.setFechaModificacion(LocalDateTime.now());

		return usuarioRolRepository.save(nuevo);
	}

	/**
	 * REVOCAR ROL - Eliminar rol permanentemente (DELETE físico)
	 */
	@Transactional
	public void revocarRol(Long usuarioId, Integer rolId, Long adminId) {

		// 1. VALIDAR QUE ES ADMIN
		validationService.validarEsAdmin(adminId);

		// 2. NO PUEDE REVOCAR SUS PROPIOS ROLES
		if (usuarioId.equals(adminId)) {
			throw new IllegalArgumentException("No puedes revocar tus propios roles");
		}

		// 3. VERIFICAR QUE EL USUARIO NO SE QUEDE SIN ROLES
		List<UsuarioRol> rolesActuales = usuarioRolRepository.findByIdUsuarioAndEstado(usuarioId, 1);

		if (rolesActuales.size() <= 1) {
			throw new IllegalArgumentException("No puedes revocar el único rol activo del usuario");
		}

		// 4. VERIFICAR QUE EXISTE
		UsuarioRolId pk = new UsuarioRolId(usuarioId, rolId);

		if (!usuarioRolRepository.existsById(pk)) {
			throw new RuntimeException("El usuario no tiene este rol");
		}

		// 5. DELETE FÍSICO
		usuarioRolRepository.deleteById(pk);
	}

	/**
	 * CAMBIAR CATEGORÍA - Promoción formal (Cliente → Mecánico, Mecánico → Admin)
	 * DELETE todos los roles + INSERT nuevo preservando fecha_creacion más antigua
	 */
	@Transactional
	public void cambiarCategoria(Long usuarioId, Integer nuevoRolId, Long adminId) {

		// 1. VALIDAR QUE ES ADMIN
		validationService.validarEsAdmin(adminId);

		// 2. NO PUEDE CAMBIAR SU PROPIA CATEGORÍA
		if (usuarioId.equals(adminId)) {
			throw new IllegalArgumentException("No puedes cambiar tu propia categoría");
		}

		// 3. OBTENER TODOS LOS ROLES ACTUALES (activos e inactivos)
		List<UsuarioRol> rolesActuales = usuarioRolRepository.findByIdUsuario(usuarioId);

		if (rolesActuales.isEmpty()) {
			throw new IllegalStateException("Usuario sin roles");
		}

		// 4. GUARDAR FECHA MÁS ANTIGUA (preservar antigüedad)
		LocalDateTime fechaIngreso = validationService.obtenerFechaIngresoSistema(usuarioId);

		// 5. VALIDAR QUE ES UN CAMBIO VÁLIDO
		List<UsuarioRol> rolesActivos = rolesActuales.stream().filter(r -> r.getEstado() == 1).toList();

		if (!rolesActivos.isEmpty()) {
			Integer categoriaActual = rolesActivos.get(0).getIdRol();
			validationService.validarCambioCategoria(categoriaActual, nuevoRolId);
		}

		// 6. DELETE TODOS LOS ROLES (activos e inactivos)
		usuarioRolRepository.deleteAll(rolesActuales);

		// 7. INSERT NUEVO ROL CON FECHA ORIGINAL
		UsuarioRol nuevoRol = new UsuarioRol(usuarioId, nuevoRolId);
		nuevoRol.setFechaCreacion(fechaIngreso); // ← PRESERVAR ANTIGÜEDAD
		nuevoRol.setEstado(1);
		nuevoRol.setFechaModificacion(LocalDateTime.now());

		usuarioRolRepository.save(nuevoRol);
	}

	/**
	 * SUSPENDER ROL - Suspensión temporal (estado=0) El rol existe pero sin acceso
	 */
	@Transactional
	public Optional<UsuarioRol> suspenderRol(Long usuarioId, Integer rolId, Long adminId) {

		validationService.validarEsAdmin(adminId);

		if (usuarioId.equals(adminId)) {
			throw new IllegalArgumentException("No puedes suspender tus propios roles");
		}

		Optional<UsuarioRol> relacion = usuarioRolRepository.findByIdUsuarioAndIdRol(usuarioId, rolId);

		if (relacion.isPresent()) {
			UsuarioRol r = relacion.get();
			r.setEstado(0); // Suspender
			r.setFechaModificacion(LocalDateTime.now());
			usuarioRolRepository.save(r);
		}

		return relacion;
	}

	/**
	 * REACTIVAR ROL - Reactivar acceso (estado=1)
	 */
	@Transactional
	public Optional<UsuarioRol> reactivarRol(Long usuarioId, Integer rolId, Long adminId) {

		validationService.validarEsAdmin(adminId);

		Optional<UsuarioRol> relacion = usuarioRolRepository.findByIdUsuarioAndIdRol(usuarioId, rolId);

		if (relacion.isPresent()) {
			UsuarioRol r = relacion.get();
			r.setEstado(1); // Reactivar
			r.setFechaModificacion(LocalDateTime.now());
			usuarioRolRepository.save(r);
		}

		return relacion;
	}

	// ═══════════════════════════════════════════════════════
	// MÉTODOS DE CONSULTA
	// ═══════════════════════════════════════════════════════

	public List<UsuarioRol> listarRolesPorUsuario(Long idUsuario) {
		return usuarioRolRepository.findByIdUsuario(idUsuario);
	}

	public List<UsuarioRol> listarRolesActivosPorUsuario(Long idUsuario) {
		return usuarioRolRepository.findByIdUsuarioAndEstado(idUsuario, 1);
	}

	public List<UsuarioRol> listarUsuariosPorRol(Integer idRol) {
		return usuarioRolRepository.findByIdRol(idRol);
	}

	public List<UsuarioRol> listarUsuariosActivosPorRol(Integer idRol) {
		return usuarioRolRepository.findByIdRolAndEstado(idRol, 1);
	}

	public List<UsuarioRol> listarActivas() {
		return usuarioRolRepository.findByEstado(1);
	}

	@Transactional(readOnly = true)
	public List<UsuarioRolDTO> listarConDetalles() {
		return usuarioRolRepository.findByEstado(1).stream()
				.map(rel -> new UsuarioRolDTO(rel.getIdUsuario(),
						rel.getUsuario() != null ? rel.getUsuario().getNombre_usuario() : null, rel.getIdRol(),
						rel.getRol() != null ? rel.getRol().getNombre() : null))
				.toList();
	}

	public Optional<UsuarioRol> buscarPorId(Long idUsuario, Integer idRol) {
		return usuarioRolRepository.findByIdUsuarioAndIdRol(idUsuario, idRol);
	}

	public List<UsuarioRol> listarTodas() {
		return usuarioRolRepository.findAll();
	}

	// ═══════════════════════════════════════════════════════
	// MÉTODOS HELPER (útiles para JWT y validaciones)
	// ═══════════════════════════════════════════════════════

	/**
	 * Obtener nombres de roles activos (para JWT)
	 */
	@Transactional(readOnly = true)
	public List<String> obtenerNombresRolesActivos(Long usuarioId) {
		return usuarioRolRepository.findByIdUsuarioAndEstado(usuarioId, 1).stream().map(ur -> ur.getRol().getNombre())
				.toList();
	}

	/**
	 * Verificar si tiene un rol específico activo
	 */
	@Transactional(readOnly = true)
	public boolean tieneRolActivo(Long usuarioId, String nombreRol) {
		return usuarioRolRepository.findByIdUsuarioAndEstado(usuarioId, 1).stream()
				.anyMatch(ur -> ur.getRol().getNombre().equalsIgnoreCase(nombreRol));
	}

	/**
	 * Obtener rol principal (mayor jerarquía)
	 */
	@Transactional(readOnly = true)
	public String obtenerRolPrincipal(Long usuarioId) {
		List<UsuarioRol> roles = usuarioRolRepository.findByIdUsuarioAndEstado(usuarioId, 1);

		// Prioridad: admin > mecanico > cliente
		if (roles.stream().anyMatch(r -> r.getIdRol() == 3))
			return "admin";
		if (roles.stream().anyMatch(r -> r.getIdRol() == 2))
			return "mecanico";
		if (roles.stream().anyMatch(r -> r.getIdRol() == 1))
			return "cliente";

		throw new IllegalStateException("Usuario sin roles activos");
	}

	// ═══════════════════════════════════════════════════════
	// MÉTODO DE USO INTERNO (sin validaciones)
	// Para migraciones, seeds, o casos especiales
	// ═══════════════════════════════════════════════════════
	public UsuarioRol crearRelacion(UsuarioRol usuarioRol) {
		usuarioRol.setEstado(1);
		usuarioRol.setFechaModificacion(LocalDateTime.now());
		return usuarioRolRepository.save(usuarioRol);
	}

	// ═══════════════════════════════════════════════════════
	// MÉTODOS LEGACY (mantener por compatibilidad)
	// ═══════════════════════════════════════════════════════

	/**
	 * @deprecated Usar revocarRol() en su lugar
	 */
	@Deprecated
	public void eliminar(Long idUsuario, Integer idRol) {
		UsuarioRolId pk = new UsuarioRolId(idUsuario, idRol);
		if (usuarioRolRepository.existsById(pk)) {
			usuarioRolRepository.deleteById(pk);
		} else {
			throw new RuntimeException("La relación usuario-rol no existe");
		}
	}

	/**
	 * @deprecated Usar suspenderRol() en su lugar
	 */
	@Deprecated
	public Optional<UsuarioRol> desactivarRelacion(Long idUsuario, Integer idRol) {
		Optional<UsuarioRol> relacion = usuarioRolRepository.findByIdUsuarioAndIdRol(idUsuario, idRol);
		if (relacion.isPresent()) {
			UsuarioRol r = relacion.get();
			r.setEstado(0);
			r.setFechaModificacion(LocalDateTime.now());
			usuarioRolRepository.save(r);
		}
		return relacion;
	}

	/**
	 * @deprecated Usar reactivarRol() en su lugar
	 */
	@Deprecated
	public Optional<UsuarioRol> activarRelacion(Long idUsuario, Integer idRol) {
		Optional<UsuarioRol> relacion = usuarioRolRepository.findByIdUsuarioAndIdRol(idUsuario, idRol);
		if (relacion.isPresent()) {
			UsuarioRol r = relacion.get();
			r.setEstado(1);
			r.setFechaModificacion(LocalDateTime.now());
			usuarioRolRepository.save(r);
		}
		return relacion;
	}
}