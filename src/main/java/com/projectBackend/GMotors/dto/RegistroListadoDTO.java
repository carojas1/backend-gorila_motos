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
    private String nombreCliente;

    // ================== MOTO ==================
    private String marcaMoto;
    private String modeloMoto;
    private String placaMoto;
    private String rutaImagenMoto;

    // ================== MANTENIMIENTO ==================
    private LocalDate fecha;
    private String descripcion;
    private String tipoMantenimiento;
    private Integer kilometraje;

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

    public String getTipo_servicio() { return tipoMantenimiento; }
    public void setTipoMantenimiento(String tipoMantenimiento) { this.tipoMantenimiento = tipoMantenimiento; }

    public Integer getKilometraje() { return kilometraje; }
    public void setKilometraje(Integer kilometraje) { this.kilometraje = kilometraje; }

    public Double getCosto_total() { return costoTotal; }
    public void setCostoTotal(Double costoTotal) { this.costoTotal = costoTotal; }

    public Long getId_factura() { return idFactura; }
    public void setIdFactura(Long idFactura) { this.idFactura = idFactura; }

    public List<DetalleFacturaDTO> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleFacturaDTO> detalles) { this.detalles = detalles; }

    public Integer getEstado() { return estado; }
    public void setEstado(Integer estado) { this.estado = estado; }
}
