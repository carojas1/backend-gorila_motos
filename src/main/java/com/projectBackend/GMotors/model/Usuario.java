package com.projectBackend.GMotors.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @Column(name = "id_usuario")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    private String nombre_completo;
    private String nombre_usuario;
    private String correo;
    private String contrasena;
    private String pais;
    private String ciudad;
    private String descripcion;
    @Column(length = 20)
    private String telefono;
    @Column(length = 20)
    private String cedula;
    @Column(length = 300)
    private String direccion;
    @JsonProperty("ruta_imagen")
    private String ruta_imagen;
    /** Puntos extra acumulados por referidos (dar o recibir un código) */
    @Column(name = "puntos_bonus", columnDefinition = "integer DEFAULT 0")
    private Integer puntosBonus = 0;
    /** Nombre de usuario del referente que usó el código → null = aún no ha usado ningún código */
    @Column(name = "codigo_referido", length = 100)
    private String codigoReferido;
    
    // Relación con UsuarioRol
    @OneToMany(mappedBy = "usuario")
    private List<UsuarioRol> roles;  // lista de roles asignados
    
    //COSTRUCTORES
    public Usuario() {}  

	public Usuario(Long id_usuario, String nombre_completo, String nombre_usuario, String correo, String contrasena,
			String pais, String ciudad, String descripcion, String ruta_imagen) {
		super();
		this.idUsuario = id_usuario;
		this.nombre_completo = nombre_completo;
		this.nombre_usuario = nombre_usuario;
		this.correo = correo;
		this.contrasena = contrasena;
		this.pais = pais;
		this.ciudad = ciudad;
		this.descripcion = descripcion;
		this.ruta_imagen = ruta_imagen;
	}

	// =====================
    // Getters y Setters
    // =====================

	public String getCorreo() {
		return correo;
	}

	public Long getId_usuario() {
		return idUsuario;
	}

	public void setId_usuario(Long id_usuario) {
		this.idUsuario = id_usuario;
	}

	public String getNombre_completo() {
		return nombre_completo;
	}

	public void setNombre_completo(String nombre_completo) {
		this.nombre_completo = nombre_completo;
	}

	public String getNombre_usuario() {
		return nombre_usuario;
	}
	
	
	public void setNombre_usuario(String nombre_usuario) {
		this.nombre_usuario = nombre_usuario;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public String getContrasena() {
		return contrasena;
	}

	public void setContrasena(String contrasena) {
		this.contrasena = contrasena;
	}

	public String getPais() {return pais;}
	public void setPais(String pais) {this.pais = pais;	}

	public String getCiudad() {return ciudad;	}
	public void setCiudad(String ciudad) {this.ciudad = ciudad;	}

	public String getDescripcion() {return descripcion;}
	public void setDescripcion(String descripcion) {this.descripcion = descripcion;}

	public String getRutaimagen() {return ruta_imagen;	}
	public void setRutaimagen(String rutaimagen) {this.ruta_imagen = rutaimagen;}

	public String getTelefono() { return telefono; }
	public void setTelefono(String telefono) { this.telefono = telefono; }

	public String getDireccion() { return direccion; }
	public void setDireccion(String direccion) { this.direccion = direccion; }

	public String getCedula() { return cedula; }
	public void setCedula(String cedula) { this.cedula = cedula; }

	public Integer getPuntosBonus() { return puntosBonus != null ? puntosBonus : 0; }
	public void setPuntosBonus(Integer puntosBonus) { this.puntosBonus = puntosBonus; }

	public String getCodigoReferido() { return codigoReferido; }
	public void setCodigoReferido(String codigoReferido) { this.codigoReferido = codigoReferido; }

	public List<UsuarioRol> getRoles() {
	    return roles;
	}

	public void setRoles(List<UsuarioRol> roles) {
	    this.roles = roles;
	}
    
}
