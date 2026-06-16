package com.projectBackend.GMotors.model;

import jakarta.persistence.*;

@Entity
@Table(name = "parametro_mantenimiento")
public class ParametroMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametro")
    private Long idParametro;

    @Column(name = "cc_min", nullable = false)
    private Integer ccMin;

    @Column(name = "cc_max")
    private Integer ccMax; // null = sin límite superior (651cc+)

    @Column(name = "tipo_mantenimiento", nullable = false, length = 60)
    private String tipoMantenimiento;

    @Column(name = "intervalo_km", nullable = false)
    private Integer intervaloKm;

    @Column(length = 255)
    private String descripcion;

    public ParametroMantenimiento() {}

    public ParametroMantenimiento(Integer ccMin, Integer ccMax, String tipo, Integer intervaloKm, String desc) {
        this.ccMin             = ccMin;
        this.ccMax             = ccMax;
        this.tipoMantenimiento = tipo;
        this.intervaloKm       = intervaloKm;
        this.descripcion       = desc;
    }

    public Long getIdParametro()                       { return idParametro; }
    public Integer getCcMin()                          { return ccMin; }
    public void setCcMin(Integer ccMin)                { this.ccMin = ccMin; }
    public Integer getCcMax()                          { return ccMax; }
    public void setCcMax(Integer ccMax)                { this.ccMax = ccMax; }
    public String getTipoMantenimiento()               { return tipoMantenimiento; }
    public void setTipoMantenimiento(String tipo)      { this.tipoMantenimiento = tipo; }
    public Integer getIntervaloKm()                    { return intervaloKm; }
    public void setIntervaloKm(Integer intervaloKm)    { this.intervaloKm = intervaloKm; }
    public String getDescripcion()                     { return descripcion; }
    public void setDescripcion(String descripcion)     { this.descripcion = descripcion; }
}
