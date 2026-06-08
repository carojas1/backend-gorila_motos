package com.projectBackend.GMotors.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "usuario_rol")
@IdClass(UsuarioRolId.class)
public class UsuarioRol {

    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Id
    @Column(name = "id_rol")
    private Integer idRol;

    // RELACIONES
    @ManyToOne
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    @JsonIgnore
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "id_rol", insertable = false, updatable = false)
    private Rol rol;

    // Columnas — fecha_creacion y fecha_modificacion son DATE en PostgreSQL → LocalDate
    @Column(name = "estado", nullable = false)
    private Integer estado = 1; // 1 = activo, 0 = inactivo

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion = LocalDate.now();

    @Column(name = "fecha_modificacion")
    private LocalDate fechaModificacion;

    // CONSTRUCTORES
    public UsuarioRol() {}

    public UsuarioRol(Long idUsuario, Integer idRol) {
        this.idUsuario = idUsuario;
        this.idRol = idRol;
        this.estado = 1;
        this.fechaCreacion = LocalDate.now();
    }

    // GETTERS & SETTERS
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }

    public Usuario getUsuario() { return usuario; }
    public Rol getRol() { return rol; }

    public Integer getEstado() { return estado; }
    public void setEstado(Integer estado) {
        this.estado = estado;
        this.fechaModificacion = LocalDate.now();
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDate getFechaCreacion() { return fechaCreacion; }

    public LocalDate getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDate fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}

