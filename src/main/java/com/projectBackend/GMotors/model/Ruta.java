package com.projectBackend.GMotors.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "rutas")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Long idRuta;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "nombre_ruta", nullable = false, length = 255)
    private String nombreRuta;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "origen_lat", nullable = false, precision = 10, scale = 8)
    private BigDecimal origenLat;

    @Column(name = "origen_lng", nullable = false, precision = 11, scale = 8)
    private BigDecimal origenLng;

    @Column(name = "destino_lat", nullable = false, precision = 10, scale = 8)
    private BigDecimal destinoLat;

    @Column(name = "destino_lng", nullable = false, precision = 11, scale = 8)
    private BigDecimal destinoLng;

    @Column(name = "distancia_km", precision = 10, scale = 2)
    private BigDecimal distanciaKm;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

  

    // Getters y Setters
    public Long getIdRuta() {
        return idRuta;
    }

    public void setIdRuta(Long idRuta) {
        this.idRuta = idRuta;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreRuta() {
        return nombreRuta;
    }

    public void setNombreRuta(String nombreRuta) {
        this.nombreRuta = nombreRuta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getOrigenLat() {
        return origenLat;
    }

    public void setOrigenLat(BigDecimal origenLat) {
        this.origenLat = origenLat;
    }

    public BigDecimal getOrigenLng() {
        return origenLng;
    }

    public void setOrigenLng(BigDecimal origenLng) {
        this.origenLng = origenLng;
    }

    public BigDecimal getDestinoLat() {
        return destinoLat;
    }

    public void setDestinoLat(BigDecimal destinoLat) {
        this.destinoLat = destinoLat;
    }

    public BigDecimal getDestinoLng() {
        return destinoLng;
    }

    public void setDestinoLng(BigDecimal destinoLng) {
        this.destinoLng = destinoLng;
    }

    public BigDecimal getDistanciaKm() {
        return distanciaKm;
    }

    public void setDistanciaKm(BigDecimal distanciaKm) {
        this.distanciaKm = distanciaKm;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

}