package com.projectBackend.GMotors.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Carga de combustible de una moto (galones). Compartido en la nube.
 * El JSON usa snake_case para coincidir con el frontend existente.
 */
@Entity
@Table(name = "carga_combustible")
public class CargaCombustible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carga")
    @JsonProperty("id")
    private Long idCarga;

    @Column(name = "id_moto", nullable = false)
    @JsonProperty("id_moto")
    private Long idMoto;

    @Column(length = 20)
    @JsonProperty("placa")
    private String placa;

    @Column(length = 10, nullable = false)   // "YYYY-MM-DD" (string, evita el array de LocalDate)
    @JsonProperty("fecha")
    private String fecha;

    @Column(precision = 10, scale = 2, nullable = false)
    @JsonProperty("litros")                  // representa GALONES (nombre conservado por compatibilidad)
    private BigDecimal litros;

    @Column(name = "costo_total", precision = 10, scale = 2)
    @JsonProperty("costo_total")
    private BigDecimal costoTotal;

    @Column(name = "km_actual")
    @JsonProperty("km_actual")
    private Integer kmActual;

    @Column(name = "km_anterior")
    @JsonProperty("km_anterior")
    private Integer kmAnterior;

    @Column(length = 255)
    @JsonProperty("notas")
    private String notas;

    public CargaCombustible() {}

    public Long getIdCarga()                  { return idCarga; }
    public void setIdCarga(Long v)            { this.idCarga = v; }
    public Long getIdMoto()                   { return idMoto; }
    public void setIdMoto(Long v)             { this.idMoto = v; }
    public String getPlaca()                  { return placa; }
    public void setPlaca(String v)            { this.placa = v; }
    public String getFecha()                  { return fecha; }
    public void setFecha(String v)            { this.fecha = v; }
    public BigDecimal getLitros()             { return litros; }
    public void setLitros(BigDecimal v)       { this.litros = v; }
    public BigDecimal getCostoTotal()         { return costoTotal; }
    public void setCostoTotal(BigDecimal v)   { this.costoTotal = v; }
    public Integer getKmActual()              { return kmActual; }
    public void setKmActual(Integer v)        { this.kmActual = v; }
    public Integer getKmAnterior()            { return kmAnterior; }
    public void setKmAnterior(Integer v)      { this.kmAnterior = v; }
    public String getNotas()                  { return notas; }
    public void setNotas(String v)            { this.notas = v; }
}
