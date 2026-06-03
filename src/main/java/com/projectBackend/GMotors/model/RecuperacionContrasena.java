package com.projectBackend.GMotors.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recuperacion_contrasena")
public class RecuperacionContrasena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRecuperacion;

    @Column(nullable = false)
    private Long idUsuario;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private String correo;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private Boolean utilizado = false;

    private LocalDateTime fechaUtilizacion;

    // Getters y Setters
    public Long getIdRecuperacion() { return idRecuperacion; }
    public void setIdRecuperacion(Long idRecuperacion) { this.idRecuperacion = idRecuperacion; }
    
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }
    
    public Boolean getUtilizado() { return utilizado; }
    public void setUtilizado(Boolean utilizado) { this.utilizado = utilizado; }
    
    public LocalDateTime getFechaUtilizacion() { return fechaUtilizacion; }
    public void setFechaUtilizacion(LocalDateTime fechaUtilizacion) { this.fechaUtilizacion = fechaUtilizacion; }

    public boolean esValido() {
        return !LocalDateTime.now().isAfter(this.fechaExpiracion) && !this.utilizado;
    }
}