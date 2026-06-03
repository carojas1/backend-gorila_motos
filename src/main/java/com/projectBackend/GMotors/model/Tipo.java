package com.projectBackend.GMotors.model;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "tipo")
public class Tipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_tipo;

    private String nombre;
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "id_producto")
    private Producto producto;
    private String concepto_manual;
    private Integer concepto_cantidad;
    private BigDecimal concepto_precio_unitario;

    // ======== GETTERS & SETTERS ========

    public Long getId_tipo() {
        return id_tipo;
    }

    public void setId_tipo(Long id_tipo) {
        this.id_tipo = id_tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public String getConcepto_manual() {
        return concepto_manual;
    }

    public void setConcepto_manual(String concepto_manual) {
        this.concepto_manual = concepto_manual;
    }

    public Integer getConcepto_cantidad() {
        return concepto_cantidad;
    }

    public void setConcepto_cantidad(Integer concepto_cantidad) {
        this.concepto_cantidad = concepto_cantidad;
    }

    public BigDecimal getConcepto_precio_unitario() {
        return concepto_precio_unitario;
    }

    public void setConcepto_precio_unitario(BigDecimal concepto_precio_unitario) {
        this.concepto_precio_unitario = concepto_precio_unitario;
    }
    
    
    // ======== MÉTODO PARA OBTENER PVP DEL PRODUCTO ========

    /**
     * Obtiene el PVP del producto asociado
     */
    public BigDecimal getProductoPvp() {
        return producto != null ? producto.getPvp() : null;
    }
    
    
    @Transient
    public Long getId_producto() {
        return producto != null ? producto.getId_producto() : null;
    }

    @Transient
    public void setId_producto(Long id_producto) {
        if (id_producto != null && producto == null) {
            producto = new Producto();
        }
        if (producto != null) {
            producto.setId_producto(id_producto);
        }
    }
}