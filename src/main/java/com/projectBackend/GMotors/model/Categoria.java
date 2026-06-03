package com.projectBackend.GMotors.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_categoria;

    private String nombre;

    private String descripcion;

    // ===== Constructor vacío =====
    public Categoria() {
    }

    // ===== Constructor con parámetros =====
    public Categoria(Long id_categoria, String nombre, String descripcion) {
        this.id_categoria = id_categoria;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    // ===== Getters y Setters =====
    public Long getId_categoria() {
        return id_categoria;
    }

    public void setId_categoria(Long id_categoria) {
        this.id_categoria = id_categoria;
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
}
