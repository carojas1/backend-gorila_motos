package com.projectBackend.GMotors.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Registro de un mantenimiento REALMENTE realizado a una moto.
 * El desgaste de cada pieza se mide desde el km del último registro de su tipo.
 */
@Entity
@Table(name = "mantenimiento_realizado")
public class MantenimientoRealizado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mantenimiento")
    private Long idMantenimiento;

    @Column(name = "id_moto", nullable = false)
    private Long idMoto;

    @Column(name = "tipo", nullable = false, length = 60)
    private String tipo; // ACEITE, FILTRO_AIRE, BUJIA, ...

    @Column(name = "km_servicio", nullable = false)
    private Integer kmServicio;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    public MantenimientoRealizado() {}

    public Long getIdMantenimiento()              { return idMantenimiento; }
    public void setIdMantenimiento(Long v)        { this.idMantenimiento = v; }
    public Long getIdMoto()                       { return idMoto; }
    public void setIdMoto(Long v)                 { this.idMoto = v; }
    public String getTipo()                       { return tipo; }
    public void setTipo(String v)                 { this.tipo = v; }
    public Integer getKmServicio()                { return kmServicio; }
    public void setKmServicio(Integer v)          { this.kmServicio = v; }
    public LocalDate getFecha()                   { return fecha; }
    public void setFecha(LocalDate v)             { this.fecha = v; }
}
