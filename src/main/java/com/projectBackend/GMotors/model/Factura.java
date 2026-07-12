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

    @Column(name = "cliente_nombre")
    @com.fasterxml.jackson.annotation.JsonProperty("cliente_nombre")
    private String clienteNombre;

    @Column(name = "cliente_cedula")
    @com.fasterxml.jackson.annotation.JsonProperty("cliente_cedula")
    private String clienteCedula;

    @Column(name = "cliente_telefono")
    @com.fasterxml.jackson.annotation.JsonProperty("cliente_telefono")
    private String clienteTelefono;

    @Column(name = "cliente_correo")
    @com.fasterxml.jackson.annotation.JsonProperty("cliente_correo")
    private String clienteCorreo;

    @Column(name = "cliente_direccion")
    @com.fasterxml.jackson.annotation.JsonProperty("cliente_direccion")
    private String clienteDireccion;

    @Column(name = "cliente_tipo")
    @com.fasterxml.jackson.annotation.JsonProperty("cliente_tipo")
    private String clienteTipo;

    @Column(name = "origen_venta")
    @com.fasterxml.jackson.annotation.JsonProperty("origen_venta")
    private String origenVenta;

    
    
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

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getClienteCedula() { return clienteCedula; }
    public void setClienteCedula(String clienteCedula) { this.clienteCedula = clienteCedula; }

    public String getClienteTelefono() { return clienteTelefono; }
    public void setClienteTelefono(String clienteTelefono) { this.clienteTelefono = clienteTelefono; }

    public String getClienteCorreo() { return clienteCorreo; }
    public void setClienteCorreo(String clienteCorreo) { this.clienteCorreo = clienteCorreo; }

    public String getClienteDireccion() { return clienteDireccion; }
    public void setClienteDireccion(String clienteDireccion) { this.clienteDireccion = clienteDireccion; }

    public String getClienteTipo() { return clienteTipo; }
    public void setClienteTipo(String clienteTipo) { this.clienteTipo = clienteTipo; }

    public String getOrigenVenta() { return origenVenta; }
    public void setOrigenVenta(String origenVenta) { this.origenVenta = origenVenta; }
    
}
