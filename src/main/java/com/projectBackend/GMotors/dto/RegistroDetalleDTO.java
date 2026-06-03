package com.projectBackend.GMotors.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.projectBackend.GMotors.model.Registro;

public class RegistroDetalleDTO {

    private Long idRegistro;
    private LocalDate fecha;
    private Integer estado;
    private String observaciones;

    // CLIENTE
    private Long idCliente;
    private String nombreCliente;

    // ENCARGADO
    private Long idEncargado;
    private String nombreEncargado;

    // MOTO
    private Long idMoto;
    private String marcaMoto;
    private String modeloMoto;
    private String placaMoto;
    private String rutaImagenMoto;

    // TIPO DE MANTENIMIENTO
    private String tipoMantenimiento;

    // FACTURA
    private Long idFactura;
    private BigDecimal costoTotal;
    
    public static RegistroDetalleDTO mapToDetalleDTO(Registro registro) {

        RegistroDetalleDTO dto = new RegistroDetalleDTO();

        dto.setIdRegistro(registro.getIdRegistro());
        dto.setFecha(registro.getFecha());
        dto.setEstado(registro.getEstado());
        dto.setObservaciones(registro.getObservaciones());

        // CLIENTE
        dto.setIdCliente(registro.getCliente().getId_usuario());
        dto.setNombreCliente(registro.getCliente().getNombre_completo());

        // ENCARGADO
        dto.setIdEncargado(registro.getEncargado().getId_usuario());
        dto.setNombreEncargado(registro.getEncargado().getNombre_completo());

        // MOTO
        dto.setIdMoto(registro.getMoto().getIdMoto());
        dto.setMarcaMoto(registro.getMoto().getMarca());
        dto.setModeloMoto(registro.getMoto().getModelo());
        dto.setPlacaMoto(registro.getMoto().getPlaca());
        dto.setRutaImagenMoto(registro.getMoto().getRutaImagenMotos());

        // TIPO DE MANTENIMIENTO ✅ NUEVO
        dto.setTipoMantenimiento(registro.getTipo().getNombre());

        // FACTURA
        dto.setIdFactura(registro.getFactura().getIdFactura());
        dto.setCostoTotal(registro.getFactura().getCostoTotal());

        return dto;
    }

    // ================= GETTERS & SETTERS =================
    
    public Long getIdRegistro() {
        return idRegistro;
    }

    public void setIdRegistro(Long idRegistro) {
        this.idRegistro = idRegistro;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
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

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public Long getIdEncargado() {
        return idEncargado;
    }

    public void setIdEncargado(Long idEncargado) {
        this.idEncargado = idEncargado;
    }

    public String getNombreEncargado() {
        return nombreEncargado;
    }

    public void setNombreEncargado(String nombreEncargado) {
        this.nombreEncargado = nombreEncargado;
    }

    public Long getIdMoto() {
        return idMoto;
    }

    public void setIdMoto(Long idMoto) {
        this.idMoto = idMoto;
    }

    public String getMarcaMoto() {
        return marcaMoto;
    }

    public void setMarcaMoto(String marcaMoto) {
        this.marcaMoto = marcaMoto;
    }

    public String getModeloMoto() {
        return modeloMoto;
    }

    public void setModeloMoto(String modeloMoto) {
        this.modeloMoto = modeloMoto;
    }

    public String getPlacaMoto() {
        return placaMoto;
    }

    public void setPlacaMoto(String placaMoto) {
        this.placaMoto = placaMoto;
    }

    public String getRutaImagenMoto() {
        return rutaImagenMoto;
    }

    public void setRutaImagenMoto(String rutaImagenMoto) {
        this.rutaImagenMoto = rutaImagenMoto;
    }

    // ✅ NUEVO - Getter y Setter para tipoMantenimiento
    public String getTipoMantenimiento() {
        return tipoMantenimiento;
    }

    public void setTipoMantenimiento(String tipoMantenimiento) {
        this.tipoMantenimiento = tipoMantenimiento;
    }

    public Long getIdFactura() {
        return idFactura;
    }

    public void setIdFactura(Long idFactura) {
        this.idFactura = idFactura;
    }

    public BigDecimal getCostoTotal() {
        return costoTotal;
    }

    public void setCostoTotal(BigDecimal costoTotal) {
        this.costoTotal = costoTotal;
    }
    
}
