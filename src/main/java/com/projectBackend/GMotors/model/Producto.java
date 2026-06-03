package com.projectBackend.GMotors.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_producto;

    private String codigo_proveedor;
    private String codigo_personal;
    private String nombre;
    private String descripcion;
    private String ruta_imagenproductos;

    @Column(name = "costo", precision = 10, scale = 2, nullable = false)
    private BigDecimal costo;

    @Column(name = "pvp", precision = 10, scale = 2, nullable = false)
    private BigDecimal pvp;
    
    private Integer stock;

    private LocalDate fecha_registro;
    private LocalDate fecha_modificacion;

    private Long id_categoria;

    // ================== GETTERS & SETTERS ==================

    public Long getId_producto() {
        return id_producto;
    }

    public void setId_producto(Long id_producto) {
        this.id_producto = id_producto;
    }

    public String getCodigo_proveedor() {
        return codigo_proveedor;
    }

    public void setCodigo_proveedor(String codigo_proveedor) {
        this.codigo_proveedor = codigo_proveedor;
    }
    
    
    public String getruta_imagenproductos() {
        return ruta_imagenproductos;
    }

    public void setruta_imagenproductos(String ruta_imagenproductos) {
        this.ruta_imagenproductos = ruta_imagenproductos;
    }
    

    public String getCodigo_personal() {
        return codigo_personal;
    }

    public void setCodigo_personal(String codigo_personal) {
        this.codigo_personal = codigo_personal;
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
    
    public BigDecimal getCosto() { return costo; }
    public void setCosto(BigDecimal costo) { this.costo = costo; }

    public BigDecimal getPvp() { return pvp; }
    public void setPvp(BigDecimal pvp) { this.pvp = pvp; }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public LocalDate getFecha_registro() {
        return fecha_registro;
    }

    public void setFecha_registro(LocalDate fecha_registro) {
        this.fecha_registro = fecha_registro;
    }

    public LocalDate getFecha_modificacion() {
        return fecha_modificacion;
    }

    public void setFecha_modificacion(LocalDate fecha_modificacion) {
        this.fecha_modificacion = fecha_modificacion;
    }

    public Long getId_categoria() {
        return id_categoria;
    }

    public void setId_categoria(Long id_categoria) {
        this.id_categoria = id_categoria;
    }
}
