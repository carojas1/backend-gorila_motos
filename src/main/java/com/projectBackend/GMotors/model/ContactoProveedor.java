package com.projectBackend.GMotors.model;

import jakarta.persistence.*;

/**
 * Contacto de un proveedor, identificado por su código (codigo_proveedor).
 * Compartido en la nube.
 */
@Entity
@Table(name = "contacto_proveedor")
public class ContactoProveedor {

    @Id
    @Column(name = "codigo", length = 80)
    private String codigo;

    @Column(length = 150)
    private String nombre;

    @Column(length = 40)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(length = 200)
    private String producto;

    public ContactoProveedor() {}

    public String getCodigo()             { return codigo; }
    public void setCodigo(String v)       { this.codigo = v; }
    public String getNombre()             { return nombre; }
    public void setNombre(String v)       { this.nombre = v; }
    public String getTelefono()           { return telefono; }
    public void setTelefono(String v)     { this.telefono = v; }
    public String getEmail()              { return email; }
    public void setEmail(String v)        { this.email = v; }
    public String getProducto()           { return producto; }
    public void setProducto(String v)     { this.producto = v; }
}
