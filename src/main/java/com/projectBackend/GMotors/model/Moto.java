package com.projectBackend.GMotors.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "motos", uniqueConstraints = @UniqueConstraint(columnNames = "placa"))
public class Moto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_moto")
	private Long idMoto;

	@Column(nullable = false, length = 20)
	private String placa;

	@Column(nullable = false)
	private Integer anio;

	@Column(nullable = false, length = 100)
	private String marca;

	@Column(nullable = false, length = 100)
	private String modelo;

	@JsonProperty("nombre_moto")
	@Column(name = "nombremoto", nullable = true, length = 100)
	private String nombreMoto;

	@JsonProperty("tipo_moto")
	@Column(name = "tipo_moto", nullable = false, length = 255)
	private String tipoMoto;
	
	@Column(nullable = false)
	private Integer kilometraje;

	@Column(nullable = false)
	private Integer cilindraje;
	
	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@JsonProperty("ruta_imagen_motos")
	@Column(name = "ruta_imagen_motos", nullable = false, length = 255)
	private String rutaImagenMotos = "Desconocido";
	
	
	// Inner Join: Por Cada placa, extraer la información del dueño ignorando algunos campos
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	@JsonIgnoreProperties({"contrasena", "correo", "descripcion", "ruta_imagen"})
	private Usuario usuario;

	// ==========================
	// CONSTRUCTORES
	// ==========================
	public Moto() {
	}

	public Moto(Long id_moto, String placa, Integer anio, String marca, String modelo, String nombreMoto, Integer kilometraje,
			Integer cilindraje, Long id_usuario, String tipoMoto, String ruta_imagenMotos) {
		super();
		this.idMoto = id_moto;
		this.placa = placa;
		this.anio = anio;
		this.marca = marca;
		this.modelo = modelo;
		this.nombreMoto = nombreMoto;
		this.kilometraje = kilometraje;
		this.cilindraje = cilindraje;
		this.tipoMoto = tipoMoto;
		this.idUsuario = id_usuario;
		this.rutaImagenMotos = ruta_imagenMotos;
	}

	// ==========================
	// GETTERS & SETTERS
	// ==========================

	/** Serializa como "id_moto" en JSON */
	public Long getId_moto() {
		return idMoto;
	}

	/** Solo uso interno Java — ignorado por Jackson */
	@JsonIgnore
	public Long getIdMoto() {
		return idMoto;
	}

	public void setIdMoto(Long idMoto) {
		this.idMoto = idMoto;
	}

	public String getPlaca() {
		return placa;
	}

	public void setPlaca(String placa) {
		this.placa = placa;
	}
	
	@JsonProperty("tipo_moto")
	public String getTipoMoto() {
		return tipoMoto;
	}

	@JsonProperty("tipo_moto")
	public void setTipoMoto(String tipoMoto) {
		this.tipoMoto = tipoMoto;
	}
	
	public String getRuta_imagenMotos() {
		return rutaImagenMotos;
	}

	public void setRuta_imagenMotos(String ruta_imagenMotos) {
		this.rutaImagenMotos = ruta_imagenMotos;
	}

	public Integer getAnio() {
		return anio;
	}

	public void setAnio(Integer anio) {
		this.anio = anio;
	}

	public String getMarca() {
		return marca;
	}

	public void setMarca(String marca) {
		this.marca = marca;
	}

	public String getModelo() {
		return modelo;
	}

	public void setModelo(String modelo) {
		this.modelo = modelo;
	}

	@JsonProperty("nombre_moto")
	public String getNombreMoto() {
	    return nombreMoto;
	}

	@JsonProperty("nombre_moto")
	public void setNombreMoto(String nombreMoto) {
	    this.nombreMoto = nombreMoto;
	}
	
	public Integer getKilometraje() {
		return kilometraje;
	}

	public void setKilometraje(Integer kilometraje) {
		this.kilometraje = kilometraje;
	}

	public Integer getCilindraje() {
		return cilindraje;
	}

	public void setCilindraje(Integer cilindraje) {
		this.cilindraje = cilindraje;
	}

	@JsonProperty("ruta_imagen_motos")
	public String getRutaImagenMotos() {
		return rutaImagenMotos;
	}

	@JsonProperty("ruta_imagen_motos")
	public void setRutaImagenMotos(String rutaImagenMotos) {
		this.rutaImagenMotos = rutaImagenMotos != null ? rutaImagenMotos : "Desconocido";
	}

	public Long getId_usuario() {
		return idUsuario;
	}

	public void setId_usuario(Long id_usuario) {
		this.idUsuario = id_usuario;
	}
	
	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	
}