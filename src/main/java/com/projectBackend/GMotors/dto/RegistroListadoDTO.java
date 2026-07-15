package com.projectBackend.GMotors.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO para listar registros de servicio.
 * Los getters usan nombres con guion bajo para que Jackson serialice como snake_case.
 */
public class RegistroListadoDTO {

    // ================== IDENTIFICACIÓN ==================
    private Long idRegistro;
    private Long idMoto;

    // ================== CLIENTE ==================
    private Long idCliente;
    private String nombreCliente;
    private String cedulaCliente;
    private String telefonoCliente;
    private String correoCliente;
    private String direccionCliente;

    // ================== ENCARGADO ==================
    private Long idEncargado;
    private String nombreEncargado;

    // ================== MOTO ==================
    private String marcaMoto;
    private String modeloMoto;
    private String placaMoto;
    private String rutaImagenMoto;

    // ================== MANTENIMIENTO ==================
    private LocalDate fecha;
    private String descripcion;
    private String observaciones;
    private String tipoMantenimiento;
    private Integer kilometraje;
    private LocalDate fechaEntregaEstimada;
    private LocalDate fechaCompletado;
    private LocalDate fechaEntregado;
    private LocalDate fechaFacturado;

    // ================== COSTO ==================
    private Double costoTotal;
    private Long idFactura;

    // ================== DETALLES FACTURA ================
    private List<DetalleFacturaDTO> detalles;

    // ================== ESTADO ==================
    private Integer estado;

    // ================== GETTERS (snake_case → JSON snake_case) ==================

    public Long getId_registro() { return idRegistro; }
    public void setIdRegistro(Long idRegistro) { this.idRegistro = idRegistro; }

    public Long getId_moto() { return idMoto; }
    public void setIdMoto(Long idMoto) { this.idMoto = idMoto; }

    public String getNombre_cliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public Long getId_cliente() { return idCliente; }
    public void setIdCliente(Long idCliente) { this.idCliente = idCliente; }

    public String getCliente_cedula() { return cedulaCliente; }
    public void setCedulaCliente(String cedulaCliente) { this.cedulaCliente = cedulaCliente; }

    public String getCliente_telefono() { return telefonoCliente; }
    public void setTelefonoCliente(String telefonoCliente) { this.telefonoCliente = telefonoCliente; }

    public String getCliente_correo() { return correoCliente; }
    public void setCorreoCliente(String correoCliente) { this.correoCliente = correoCliente; }

    public String getCliente_direccion() { return direccionCliente; }
    public void setDireccionCliente(String direccionCliente) { this.direccionCliente = direccionCliente; }

    public Long getId_encargado() { return idEncargado; }
    public void setIdEncargado(Long idEncargado) { this.idEncargado = idEncargado; }

    public String getNombre_encargado() { return nombreEncargado; }
    public void setNombreEncargado(String nombreEncargado) { this.nombreEncargado = nombreEncargado; }

    public String getMarca_moto() { return marcaMoto; }
    public void setMarcaMoto(String marcaMoto) { this.marcaMoto = marcaMoto; }

    public String getModelo_moto() { return modeloMoto; }
    public void setModeloMoto(String modeloMoto) { this.modeloMoto = modeloMoto; }

    /** Se expone como "placa" (no "placa_moto") para coincidir con el frontend */
    public String getPlaca() { return placaMoto; }
    public void setPlacaMoto(String placaMoto) { this.placaMoto = placaMoto; }

    public String getRuta_imagen_moto() { return rutaImagenMoto; }
    public void setRutaImagenMoto(String rutaImagenMoto) { this.rutaImagenMoto = rutaImagenMoto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getTipo_servicio() { return tipoMantenimiento; }
    public void setTipoMantenimiento(String tipoMantenimiento) { this.tipoMantenimiento = tipoMantenimiento; }

    public Integer getKilometraje() { return kilometraje; }
    public void setKilometraje(Integer kilometraje) { this.kilometraje = kilometraje; }

    public LocalDate getFecha_entrega_estimada() { return fechaEntregaEstimada; }
    public void setFechaEntregaEstimada(LocalDate fechaEntregaEstimada) { this.fechaEntregaEstimada = fechaEntregaEstimada; }

    public LocalDate getFecha_completado() { return fechaCompletado; }
    public void setFechaCompletado(LocalDate fechaCompletado) { this.fechaCompletado = fechaCompletado; }

    public LocalDate getFecha_entregado() { return fechaEntregado; }
    public void setFechaEntregado(LocalDate fechaEntregado) { this.fechaEntregado = fechaEntregado; }

    public LocalDate getFecha_facturado() { return fechaFacturado; }
    public void setFechaFacturado(LocalDate fechaFacturado) { this.fechaFacturado = fechaFacturado; }

    public Double getCosto_total() { return costoTotal; }
    public void setCostoTotal(Double costoTotal) { this.costoTotal = costoTotal; }

    public Long getId_factura() { return idFactura; }
    public void setIdFactura(Long idFactura) { this.idFactura = idFactura; }

    public List<DetalleFacturaDTO> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleFacturaDTO> detalles) { this.detalles = detalles; }

    public Integer getEstado() { return estado; }
    public void setEstado(Integer estado) { this.estado = estado; }
}
