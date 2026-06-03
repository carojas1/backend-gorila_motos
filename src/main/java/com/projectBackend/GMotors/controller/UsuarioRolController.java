package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.dto.UsuarioRolDTO;
import com.projectBackend.GMotors.model.UsuarioRol;
import com.projectBackend.GMotors.service.UsuarioRolService;
import com.projectBackend.GMotors.service.RoleValidationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller para gestión de roles de usuario
 */
@RestController
@RequestMapping("/api/usuario_rol")
public class UsuarioRolController {

    @Autowired
    private UsuarioRolService usuarioRolService;

    @Autowired
    private RoleValidationService validationService;

    // ═══════════════════════════════════════════════════════
    // OPERACIONES PRINCIPALES
    // ═══════════════════════════════════════════════════════

    /**
     * ASIGNAR ROL
     * POST /api/usuario_rol/asignar
     * Body: { "usuarioId": 1, "rolId": 2, "adminId": 99 }
     */
    @PostMapping("/asignar")
    public ResponseEntity<?> asignarRol(@RequestBody AsignarRolRequest request) {
        try {
            UsuarioRol relacion = usuarioRolService.asignarRol(
                    request.getUsuarioId(),
                    request.getRolId(),
                    request.getAdminId()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Rol asignado correctamente",
                    "usuarioId", relacion.getIdUsuario(),
                    "rolId", relacion.getIdRol(),
                    "estado", relacion.getEstado()
            ));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al asignar rol: " + e.getMessage()));
        }
    }

    /**
     * REVOCAR ROL
     * DELETE /api/usuario_rol/revocar
     * Body: { "usuarioId": 1, "rolId": 2, "adminId": 99 }
     */
    @DeleteMapping("/revocar")
    public ResponseEntity<?> revocarRol(@RequestBody RevocarRolRequest request) {
        try {
            usuarioRolService.revocarRol(
                    request.getUsuarioId(),
                    request.getRolId(),
                    request.getAdminId()
            );

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Rol revocado correctamente",
                    "usuarioId", request.getUsuarioId(),
                    "rolId", request.getRolId()
            ));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al revocar rol: " + e.getMessage()));
        }
    }

    /**
     * CAMBIAR CATEGORÍA
     * POST /api/usuario_rol/cambiar-categoria
     * Body: { "usuarioId": 1, "nuevoRolId": 2, "adminId": 99 }
     */
    @PostMapping("/cambiar-categoria")
    public ResponseEntity<?> cambiarCategoria(@RequestBody CambiarCategoriaRequest request) {
        try {
            usuarioRolService.cambiarCategoria(
                    request.getUsuarioId(),
                    request.getNuevoRolId(),
                    request.getAdminId()
            );

            List<String> nuevosRoles =
                    usuarioRolService.obtenerNombresRolesActivos(request.getUsuarioId());

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Categoría cambiada correctamente",
                    "usuarioId", request.getUsuarioId(),
                    "nuevoRolId", request.getNuevoRolId(),
                    "rolesActuales", nuevosRoles
            ));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cambiar categoría: " + e.getMessage()));
        }
    }

    /**
     * SUSPENDER ROL
     * PUT /api/usuario_rol/suspender
     * Body: { "usuarioId": 1, "rolId": 2, "adminId": 99 }
     */
    @PutMapping("/suspender")
    public ResponseEntity<?> suspenderRol(@RequestBody SuspenderRolRequest request) {
        try {
            Optional<UsuarioRol> result = usuarioRolService.suspenderRol(
                    request.getUsuarioId(),
                    request.getRolId(),
                    request.getAdminId()
            );

            return result
                    .<ResponseEntity<?>>map(usuarioRol -> ResponseEntity.ok(Map.of(
                            "mensaje", "Rol suspendido correctamente",
                            "usuarioId", request.getUsuarioId(),
                            "rolId", request.getRolId(),
                            "estado", 0
                    )))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Relación usuario-rol no encontrada")));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al suspender rol: " + e.getMessage()));
        }
    }

    /**
     * REACTIVAR ROL
     * PUT /api/usuario_rol/reactivar
     * Body: { "usuarioId": 1, "rolId": 2, "adminId": 99 }
     */
    @PutMapping("/reactivar")
    public ResponseEntity<?> reactivarRol(@RequestBody ReactivarRolRequest request) {
        try {
            Optional<UsuarioRol> result = usuarioRolService.reactivarRol(
                    request.getUsuarioId(),
                    request.getRolId(),
                    request.getAdminId()
            );

            return result
                    .<ResponseEntity<?>>map(usuarioRol -> ResponseEntity.ok(Map.of(
                            "mensaje", "Rol reactivado correctamente",
                            "usuarioId", request.getUsuarioId(),
                            "rolId", request.getRolId(),
                            "estado", 1
                    )))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Relación usuario-rol no encontrada")));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al reactivar rol: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════
    // ENDPOINTS DE CONSULTA (SIN CAMBIOS)
    // ═══════════════════════════════════════════════════════

    @GetMapping("/{idUsuario}/{idRol}")
    public ResponseEntity<UsuarioRol> obtenerPorId(
            @PathVariable Long idUsuario,
            @PathVariable Integer idRol) {

        Optional<UsuarioRol> relacion = usuarioRolService.buscarPorId(idUsuario, idRol);
        return relacion.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<UsuarioRol>> listarTodas() {
        return ResponseEntity.ok(usuarioRolService.listarTodas());
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<UsuarioRol>> obtenerRolesPorUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(usuarioRolService.listarRolesPorUsuario(idUsuario));
    }

    @GetMapping("/usuario/{idUsuario}/activos")
    public ResponseEntity<List<UsuarioRol>> obtenerRolesActivosPorUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(usuarioRolService.listarRolesActivosPorUsuario(idUsuario));
    }

    @GetMapping("/rol/{idRol}")
    public ResponseEntity<List<UsuarioRol>> obtenerUsuariosPorRol(@PathVariable Integer idRol) {
        return ResponseEntity.ok(usuarioRolService.listarUsuariosPorRol(idRol));
    }

    @GetMapping("/rol/{idRol}/activos")
    public ResponseEntity<List<UsuarioRol>> obtenerUsuariosActivosPorRol(@PathVariable Integer idRol) {
        return ResponseEntity.ok(usuarioRolService.listarUsuariosActivosPorRol(idRol));
    }

    @GetMapping("/activos")
    public ResponseEntity<List<UsuarioRol>> listarActivos() {
        return ResponseEntity.ok(usuarioRolService.listarActivas());
    }

    @GetMapping("/detalles")
    public ResponseEntity<List<UsuarioRolDTO>> listarConDetalles() {
        return ResponseEntity.ok(usuarioRolService.listarConDetalles());
    }

    @GetMapping("/usuario/{idUsuario}/nombres")
    public ResponseEntity<List<String>> obtenerNombresRoles(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(
                usuarioRolService.obtenerNombresRolesActivos(idUsuario)
        );
    }

    @GetMapping("/usuario/{idUsuario}/principal")
    public ResponseEntity<?> obtenerRolPrincipal(@PathVariable Long idUsuario) {
        try {
            return ResponseEntity.ok(Map.of(
                    "rolPrincipal",
                    usuarioRolService.obtenerRolPrincipal(idUsuario)
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/usuario/{idUsuario}/requiere-cambio/{rolId}")
    public ResponseEntity<?> requiereCambioCategoria(
            @PathVariable Long idUsuario,
            @PathVariable Long rolId) {

        boolean requiere = validationService.requiereCambioCategoria(idUsuario, rolId);

        return ResponseEntity.ok(Map.of(
                "requiereCambioCategoria", requiere,
                "mensaje", requiere
                        ? "Este usuario requiere cambio formal de categoría"
                        : "Puede usar asignarRol directamente"
        ));
    }
}

/* ═══════════════════════════════════════════════════════
   REQUEST DTOs
   ═══════════════════════════════════════════════════════ */

class AsignarRolRequest {
    private Long usuarioId;
    private Integer rolId;
    private Long adminId;

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Integer getRolId() { return rolId; }
    public void setRolId(Integer rolId) { this.rolId = rolId; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
}

class RevocarRolRequest {
    private Long usuarioId;
    private Integer rolId;
    private Long adminId;

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Integer getRolId() { return rolId; }
    public void setRolId(Integer rolId) { this.rolId = rolId; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
}

class CambiarCategoriaRequest {
    private Long usuarioId;
    private Integer nuevoRolId;
    private Long adminId;

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Integer getNuevoRolId() { return nuevoRolId; }
    public void setNuevoRolId(Integer nuevoRolId) { this.nuevoRolId = nuevoRolId; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
}

class SuspenderRolRequest {
    private Long usuarioId;
    private Integer rolId;
    private Long adminId;

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Integer getRolId() { return rolId; }
    public void setRolId(Integer rolId) { this.rolId = rolId; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
}

class ReactivarRolRequest {
    private Long usuarioId;
    private Integer rolId;
    private Long adminId;

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Integer getRolId() { return rolId; }
    public void setRolId(Integer rolId) { this.rolId = rolId; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
}
