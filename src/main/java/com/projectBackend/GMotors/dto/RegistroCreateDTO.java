package com.projectBackend.GMotors.dto;

import java.util.List;

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

    public List<DetalleFacturaCreateDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleFacturaCreateDTO> detalles) {
        this.detalles = detalles;
    }
}
