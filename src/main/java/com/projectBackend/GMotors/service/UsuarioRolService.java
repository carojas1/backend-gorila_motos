package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.dto.UsuarioRolDTO;
import com.projectBackend.GMotors.model.UsuarioRol;
import com.projectBackend.GMotors.model.UsuarioRolId;
import com.projectBackend.GMotors.repository.UsuarioRolRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
			relacion.setFechaModificacion(LocalDate.now());
			return usuarioRolRepository.save(relacion);
		}

		// 5. CREAR NUEVO ROL
		UsuarioRol nuevo = new UsuarioRol(usuarioId, rolId);
		nuevo.setEstado(1);
		nuevo.setFechaModificacion(LocalDate.now());

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
	 * CAMBIAR CATEGORÍA - El admin asigna el rol que decida a cualquier usuario.
	 * Desactiva todos los roles actuales y activa/crea el nuevo (un solo rol activo).
	 * Robusto: funciona aunque el usuario no tenga roles (sin-rol) o ya tenga el
	 * mismo rol (idempotente). No bloquea ninguna transición — el admin manda.
	 */
	@Transactional
	public void cambiarCategoria(Long usuarioId, Integer nuevoRolId, Long adminId) {

		// 1. VALIDAR QUE ES ADMIN
		validationService.validarEsAdmin(adminId);

		// 2. NO PUEDE CAMBIAR SU PROPIA CATEGORÍA
		if (usuarioId.equals(adminId)) {
			throw new IllegalArgumentException("No puedes cambiar tu propia categoría");
		}

		// 3. VALIDAR ENTRADA
		if (nuevoRolId == null) {
			throw new IllegalArgumentException("Debes indicar el nuevo rol");
		}

		// 4. OBTENER TODOS LOS ROLES ACTUALES (puede estar vacío: usuario sin rol)
		List<UsuarioRol> rolesActuales = usuarioRolRepository.findByIdUsuario(usuarioId);

		// 5. PRESERVAR FECHA DE INGRESO (o hoy si es nuevo)
		LocalDate fechaIngreso = rolesActuales.isEmpty()
				? LocalDate.now()
				: validationService.obtenerFechaIngresoSistema(usuarioId);

		// 6. DESACTIVAR TODOS LOS ROLES ACTUALES (soft-delete: estado=0)
		if (!rolesActuales.isEmpty()) {
			rolesActuales.forEach(r -> {
				r.setEstado(0);
				r.setFechaModificacion(LocalDate.now());
			});
			usuarioRolRepository.saveAll(rolesActuales);
		}

		// 7. ACTIVAR O CREAR EL NUEVO ROL (admin decide — sin restricciones de transición)
		rolesActuales.stream()
			.filter(r -> r.getIdRol().equals(nuevoRolId))
			.findFirst()
			.ifPresentOrElse(existente -> {
				existente.setEstado(1);
				existente.setFechaCreacion(fechaIngreso);
				existente.setFechaModificacion(LocalDate.now());
				usuarioRolRepository.save(existente);
			}, () -> {
				UsuarioRol nuevo = new UsuarioRol(usuarioId, nuevoRolId);
				nuevo.setFechaCreacion(fechaIngreso);
				nuevo.setEstado(1);
				nuevo.setFechaModificacion(LocalDate.now());
				usuarioRolRepository.save(nuevo);
			});
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
			r.setFechaModificacion(LocalDate.now());
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
			r.setFechaModificacion(LocalDate.now());
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

		// Prioridad: admin(1) > mecanico(3) > cliente(2) — según seeder
		if (roles.stream().anyMatch(r -> r.getIdRol() == 1))
			return "admin";
		if (roles.stream().anyMatch(r -> r.getIdRol() == 3))
			return "mecanico";
		if (roles.stream().anyMatch(r -> r.getIdRol() == 2))
			return "cliente";

		throw new IllegalStateException("Usuario sin roles activos");
	}

	// ═══════════════════════════════════════════════════════
	// MÉTODO DE USO INTERNO (sin validaciones)
	// Para migraciones, seeds, o casos especiales
	// ═══════════════════════════════════════════════════════
	public UsuarioRol crearRelacion(UsuarioRol usuarioRol) {
		usuarioRol.setEstado(1);
		usuarioRol.setFechaModificacion(LocalDate.now());
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
			r.setFechaModificacion(LocalDate.now());
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
			r.setFechaModificacion(LocalDate.now());
			usuarioRolRepository.save(r);
		}
		return relacion;
	}
}