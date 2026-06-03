package com.projectBackend.GMotors.model;

import java.io.Serializable;
import java.util.Objects;

//UsuarioRolId: Clase auxiliar que representa la PK compuesta (id_usuario, id_rol)
public class UsuarioRolId implements Serializable {

    private Long idUsuario;
    private Integer idRol;

    public UsuarioRolId() {}
    public UsuarioRolId(Long idUsuario, Integer idRol) {
        this.idUsuario = idUsuario;
        this.idRol = idRol;
    }
    // GETTERS Y SETTERS
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
    
    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioRolId)) return false;
        UsuarioRolId that = (UsuarioRolId) o;
        return Objects.equals(idUsuario, that.idUsuario) &&
               Objects.equals(idRol, that.idRol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuario, idRol);
    }
    //toString() para debugging
    @Override
    public String toString() {
        return "UsuarioRolId{usuario=" + idUsuario + ", rol=" + idRol + "}";
    }
    
}
