package com.projectBackend.GMotors.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Rol {

    @Id
    @Column(name = "id_rol")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_rol;

    private String nombre;
    
    //Relacion con Usuario
    @OneToMany(mappedBy = "rol")
    private List<UsuarioRol> usuarios;
    // ======== Constructores ========
    public Rol() {
    }

    public Rol(Long id_rol, String nombre) {
        this.id_rol = id_rol;
        this.nombre = nombre;
    }

    // ======== Getters y Setters ========

    public Long getId_rol() {
        return id_rol;
    }

    public void setId_rol(Long id_rol) {
        this.id_rol = id_rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
