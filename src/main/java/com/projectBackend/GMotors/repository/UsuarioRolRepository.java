package com.projectBackend.GMotors.repository;

import com.projectBackend.GMotors.model.UsuarioRol;
import com.projectBackend.GMotors.model.UsuarioRolId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/*
 * ¿Por qué JpaRepository<UsuarioRol, UsuarioRolId>?
 * 
 * Porque:
 * 
 * El primer parámetro (UsuarioRol) es la ENTIDAD
 * 
 * El segundo parámetro (UsuarioRolId) es el TIPO DE LA CLAVE PRIMARIA
 * 
 * Como usuario_rol tiene una PK compuesta (id_usuario, id_rol),
 * necesitamos una clase especial (UsuarioRolId) para representarla.
 */
@Repository
public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UsuarioRolId> {

	// 🔹 Buscar todas las relaciones ACTIVAS por usuario
	List<UsuarioRol> findByIdUsuarioAndEstado(Long userId, Integer estado);

	// 🔹 Buscar todas las relaciones ACTIVAS por rol
	List<UsuarioRol> findByIdRolAndEstado(Integer idRol, Integer estado);

	// 🔹 Buscar relación usuario + rol sin importar estado
	Optional<UsuarioRol> findByIdUsuarioAndIdRol(Long idUsuario, Integer idRol);

	// 🔹 Buscar relación usuario + rol SOLO si está activa
	Optional<UsuarioRol> findByIdUsuarioAndIdRolAndEstado(Integer idUsuario, Integer idRol, Integer estado);

	// 🔹 Listar TODAS las relaciones activas (útil para mostrar en el front)
	List<UsuarioRol> findByEstado(Integer estado);

	//---------
	// Buscar todas las relaciones por id_usuario
	List<UsuarioRol> findByIdUsuario(Long usuarioId);

	// Buscar todas las relaciones por id_rol
	List<UsuarioRol> findByIdRol(Integer idRol);
}

