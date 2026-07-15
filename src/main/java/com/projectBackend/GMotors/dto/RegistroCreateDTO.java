package com.projectBackend.GMotors.dto;

import java.util.List;
import java.time.LocalDate;

public class RegistroCreateDTO {

    // --- RELACIONES ---
    private Long idCliente;
    private Long idEncargado;
    private Long idMoto;
    private Long idTipo;

    // --- REGISTRO ---
    private Integer estado;
    private String observaciones;
    private Integer kilometraje;
    private LocalDate fecha;
    private LocalDate fechaEntregaEstimada;

    // --- FACTURA ---
    private List<DetalleFacturaCreateDTO> detalles;

    // getters y setters

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public Long getIdEncargado() {
        return idEncargado;
    }

    public void setIdEncargado(Long idEncargado) {
        this.idEncargado = idEncargado;
    }

    public Long getIdMoto() {
        return idMoto;
    }

    public void setIdMoto(Long idMoto) {
        this.idMoto = idMoto;
    }

    public Long getIdTipo() {
        return idTipo;
    }

    public void setIdTipo(Long idTipo) {
        this.idTipo = idTipo;
    }

    public Integer getEstado() {
        return estado;
    }

    public void setEstado(Integer estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Integer getKilometraje() { return kilometraje; }
    public void setKilometraje(Integer kilometraje) { this.kilometraje = kilometraje; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalDate getFechaEntregaEstimada() { return fechaEntregaEstimada; }
    public void setFechaEntregaEstimada(LocalDate fechaEntregaEstimada) { this.fechaEntregaEstimada = fechaEntregaEstimada; }

    public List<DetalleFacturaCreateDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleFacturaCreateDTO> detalles) {
        this.detalles = detalles;
    }
}
