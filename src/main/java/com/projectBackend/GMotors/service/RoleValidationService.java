package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.model.UsuarioRol;
import com.projectBackend.GMotors.repository.UsuarioRolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Servicio de validación de roles y permisos
 * Centraliza todas las reglas de negocio para gestión de roles
 */
@Service
public class RoleValidationService {
    
    @Autowired
    private UsuarioRolRepository usuarioRolRepository;
    
    // ═══════════════════════════════════════════════════════
    // CONSTANTES DE ROLES (según BD)
    // ═══════════════════════════════════════════════════════
    private static final int ROL_ADMIN = 1;
    private static final int ROL_CLIENTE = 2;
    private static final int ROL_MECANICO = 3;
    
    // ═══════════════════════════════════════════════════════
    // VALIDACIONES DE PERMISOS
    // ═══════════════════════════════════════════════════════
    
    /**
     * Verifica si el usuario tiene rol ADMIN activo
     */
    public boolean esAdmin(Long userId) {
        List<UsuarioRol> roles = usuarioRolRepository.findByIdUsuarioAndEstado(userId, 1);
        
        // 🔍 DEBUG TEMPORAL
        System.out.println("🔍 Verificando admin para usuario: " + userId);
        System.out.println("   Roles activos encontrados: " + roles.size());
        roles.forEach(ur -> System.out.println("   - Rol ID: " + ur.getIdRol()));
        
        boolean resultado = roles.stream()
            .anyMatch(ur -> ur.getIdRol() == ROL_ADMIN);
        
        System.out.println("   ¿Es Admin?: " + resultado);
        
        return resultado;
    }
    
    /**
     * Valida que quien intenta gestionar roles sea admin
     * Lanza SecurityException si no es admin
     */
    public void validarEsAdmin(Long adminId) {
        if (!esAdmin(adminId)) {
            throw new SecurityException(
                "Solo administradores pueden gestionar roles"
            );
        }
    }
    
    // ═══════════════════════════════════════════════════════
    // OBTENCIÓN DE INFORMACIÓN HISTÓRICA
    // ═══════════════════════════════════════════════════════
    
    /**
     * Obtiene el primer rol asignado (cronológicamente) del usuario
     * Busca en TODOS los roles (activos e inactivos)
     */
    public Integer obtenerPrimerRol(Long usuarioId) {
        List<UsuarioRol> roles = usuarioRolRepository.findByIdUsuario(usuarioId);
        
        if (roles.isEmpty()) {
            throw new IllegalStateException("Usuario sin roles");
        }
        
        // El más antiguo por fecha_creacion
        UsuarioRol primero = roles.stream()
            .min(Comparator.comparing(UsuarioRol::getFechaCreacion))
            .get();
        
        return primero.getIdRol();
    }
    
    /**
     * Verifica si el usuario empezó como cliente (categoría externa)
     * CRÍTICO para prevenir escalado no autorizado
     */
    public boolean empezóComoCliente(Long usuarioId) {
        Integer primerRol = obtenerPrimerRol(usuarioId);
        return primerRol == ROL_CLIENTE;
    }
    
    /**
     * Obtiene la fecha_creacion más antigua de los roles del usuario
     * Usado para preservar antigüedad en cambios de categoría
     */
    public LocalDateTime obtenerFechaIngresoSistema(Long usuarioId) {
        List<UsuarioRol> roles = usuarioRolRepository.findByIdUsuario(usuarioId);
        
        if (roles.isEmpty()) {
            return LocalDateTime.now();
        }
        
        return roles.stream()
            .map(UsuarioRol::getFechaCreacion)
            .min(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
    }
    
    // ═══════════════════════════════════════════════════════
    // VALIDACIONES DE COMBINACIÓN DE ROLES
    // ═══════════════════════════════════════════════════════
    
    /**
     * Valida que la combinación de roles sea permitida según reglas del taller
     * 
     * REGLAS:
     * 1. Si empezaste como CLIENTE → No puedes escalar a roles de trabajo (Admin/Mecánico)
     * 2. No puedes duplicar roles activos
     * 3. TALLER PEQUEÑO: Admin + Mecánico permitido, Mecánico + Cliente permitido, etc.
     * 
     * @throws IllegalArgumentException si la combinación no es válida
     */
    public void validarCombinacionRoles(Long usuarioId, Long nuevoRolId) {
        
        // Obtener roles ACTIVOS actuales
        List<UsuarioRol> rolesActivos = usuarioRolRepository
            .findByIdUsuarioAndEstado(usuarioId, 1);
        
        List<Integer> idsRolesActivos = rolesActivos.stream()
            .map(UsuarioRol::getIdRol)
            .toList();
        
        // ═════════════════════════════════════════════════════
        // REGLA 1: Si empezaste como CLIENTE → No puedes escalar a roles de trabajo
        // ═════════════════════════════════════════════════════
        if (empezóComoCliente(usuarioId) && (nuevoRolId == ROL_ADMIN || nuevoRolId == ROL_MECANICO)) {
            throw new IllegalArgumentException(
                "Usuario registrado como cliente no puede obtener roles de trabajo (Admin/Mecánico). " +
                "Use 'cambiarCategoria' para una promoción formal."
            );
        }
        
        // ═════════════════════════════════════════════════════
        // REGLA 2: No duplicar roles activos
        // ═════════════════════════════════════════════════════
        if (idsRolesActivos.contains(nuevoRolId)) {
            throw new IllegalArgumentException(
                "El usuario ya tiene este rol activo"
            );
        }
        
        // ═════════════════════════════════════════════════════
        // TODO LO DEMÁS PERMITIDO (para taller pequeño)
        // ═════════════════════════════════════════════════════
        // ✅ Mecánico + Cliente (empleado con motos)
        // ✅ Admin + Cliente (dueño con motos)
        // ✅ Admin + Mecánico (dueño que también repara)
        // ✅ Admin + Mecánico + Cliente (super usuario)
    }
    
    // ═══════════════════════════════════════════════════════
    // VALIDACIONES DE CAMBIO DE CATEGORÍA
    // ═══════════════════════════════════════════════════════
    
    /**
     * Valida que el cambio de categoría sea válido
     * (Cliente → Empleado, Mecánico → Admin, etc.)
     * 
     * @throws IllegalArgumentException si no es un cambio válido
     */
    public void validarCambioCategoria(Integer categoriaActual, Integer categoriaNueva) {
        
        // Obtener nombres de categorías
        String actual = obtenerNombreCategoria(categoriaActual);
        String nueva = obtenerNombreCategoria(categoriaNueva);
        
        // No puede ser la misma categoría
        if (actual.equals(nueva)) {
            throw new IllegalArgumentException(
                "No es un cambio de categoría válido (misma categoría: " + actual + ")"
            );
        }
        
        // Validaciones adicionales opcionales:
        // Por ahora, cualquier cambio entre diferentes categorías es válido
        // Puedes agregar reglas específicas aquí si es necesario
    }
    
    /**
     * Determina la categoría de un rol
     * Admin = empleado gerencial
     * Cliente = consumidor externo
     * Mecánico = empleado operativo
     */
    private String obtenerNombreCategoria(Integer rolId) {
        return switch (rolId) {
            case ROL_ADMIN -> "admin";
            case ROL_CLIENTE -> "cliente";
            case ROL_MECANICO -> "mecanico";
            default -> "desconocido";
        };
    }
    
    /**
     * Obtiene el nombre legible del rol
     */
    public String obtenerNombreRol(Integer rolId) {
        return switch (rolId) {
            case ROL_ADMIN -> "Administrador";
            case ROL_CLIENTE -> "Cliente";
            case ROL_MECANICO -> "Mecánico";
            default -> "Desconocido";
        };
    }
    
    // ═══════════════════════════════════════════════════════
    // MÉTODOS HELPER ADICIONALES
    // ═══════════════════════════════════════════════════════
    
    /**
     * Verifica si un usuario tiene un rol específico activo
     */
    public boolean tieneRolActivo(Long usuarioId, Integer rolId) {
        return usuarioRolRepository.findByIdUsuarioAndEstado(usuarioId, 1)
            .stream()
            .anyMatch(ur -> ur.getIdRol().equals(rolId));
    }
    
    /**
     * Verifica si un usuario tiene algún rol activo
     */
    public boolean tieneRolesActivos(Long usuarioId) {
        List<UsuarioRol> roles = usuarioRolRepository
            .findByIdUsuarioAndEstado(usuarioId, 1);
        return !roles.isEmpty();
    }
    
    /**
     * Obtiene la categoría base del usuario (cliente o empleado)
     * Basado en su primer rol histórico
     */
    public String obtenerCategoriaBase(Long usuarioId) {
        Integer primerRol = obtenerPrimerRol(usuarioId);
        
        return switch (primerRol) {
            case ROL_CLIENTE -> "CONSUMIDOR_EXTERNO";          // Cliente
            case ROL_ADMIN, ROL_MECANICO -> "PERSONAL_INTERNO"; // Admin o Mecánico
            default -> "DESCONOCIDO";
        };
    }
    
    /**
     * Verifica si un cambio de roles requiere "cambiarCategoria" en lugar de "asignarRol"
     * Por ejemplo: Cliente → Mecánico requiere cambio formal
     */
    public boolean requiereCambioCategoria(Long usuarioId, Long nuevoRolId) {
        try {
            validarCombinacionRoles(usuarioId, nuevoRolId);
            return false;  // No requiere cambio, puede usar asignarRol
        } catch (IllegalArgumentException e) {
            // Si la validación falla porque empezó como cliente
            if (e.getMessage().contains("cambiarCategoria")) {
                return true;  // Requiere cambio formal de categoría
            }
            throw e;  // Otro tipo de error, re-lanzar
        }
    }
    
    /**
     * Verifica si es un usuario de tipo Cliente
     */
    public boolean esCliente(Long usuarioId) {
        return tieneRolActivo(usuarioId, ROL_CLIENTE);
    }
    
    /**
     * Verifica si es un usuario de tipo Mecánico
     */
    public boolean esMecanico(Long usuarioId) {
        return tieneRolActivo(usuarioId, ROL_MECANICO);
    }
    
    /**
     * Obtiene constante ROL_ADMIN para uso externo
     */
    public int getRolAdmin() {
        return ROL_ADMIN;
    }
    
    /**
     * Obtiene constante ROL_CLIENTE para uso externo
     */
    public int getRolCliente() {
        return ROL_CLIENTE;
    }
    
    /**
     * Obtiene constante ROL_MECANICO para uso externo
     */
    public int getRolMecanico() {
        return ROL_MECANICO;
    }

	public void validarCombinacionRoles(Long usuarioId, Integer rolId) {
		// TODO Auto-generated method stub
		
	}
}