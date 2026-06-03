
package com.projectBackend.GMotors.dto;

public class UsuarioRolDTO {
    private Long idUsuario;
    private String nombreUsuario;
    private Integer idRol;
    private String nombreRol;

    public UsuarioRolDTO(Long idUsuario, String nombreUsuario, Integer idRol, String nombreRol) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.idRol = idRol;
        this.nombreRol = nombreRol;
    }

    // getters (y setters si los necesitas)
    public Long getIdUsuario() { return idUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public Integer getIdRol() { return idRol; }
    public String getNombreRol() { return nombreRol; }
}
