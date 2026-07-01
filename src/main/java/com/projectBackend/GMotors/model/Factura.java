package com.projectBackend.GMotors.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura")
    @com.fasterxml.jackson.annotation.JsonProperty("id_factura")
    private Long idFactura;

    @Column(name = "fecha_emision", nullable = false)
    @com.fasterxml.jackson.annotation.JsonProperty("fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "id_usuario", nullable = false)
    @com.fasterxml.jackson.annotation.JsonProperty("id_usuario")
    private Long idUsuario;

    @Column(
        name = "costo_total",
        precision = 10,
        scale = 2,
        nullable = false
    )
    @com.fasterxml.jackson.annotation.JsonProperty("costo_total")
    private BigDecimal costoTotal = BigDecimal.ZERO;

    
    
    // ================== GETTERS & SETTERS ==================

    public Long getIdFactura() {
        return idFactura;
    }

    public void setIdFactura(Long idFactura) {
        this.idFactura = idFactura;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public BigDecimal getCostoTotal() {
        return costoTotal;
    }

    public void setCostoTotal(BigDecimal costoTotal) {
        this.costoTotal = costoTotal;
    }
    
}
