package com.projectBackend.GMotors.model;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "detalles_factura")
public class DetalleFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_detalle;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "id_factura", nullable = false)
    private Long idFactura;

    @Column(nullable = true)
    private Long idProducto;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(length = 255)
    private String descripcion;

    // ================== GETTERS & SETTERS ==================

    public Long getId_detalle() {
        return id_detalle;
    }

    public void setId_detalle(Long id_detalle) {
        this.id_detalle = id_detalle;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Long getIdFactura() {
        return idFactura;
    }

    public void setIdFactura(Long idFactura) {
        this.idFactura = idFactura;
    }

    public Long getId_producto() {
        return idProducto;
    }

    public void setId_producto(Long id_producto) {
        this.idProducto = id_producto;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
