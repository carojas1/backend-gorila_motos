package com.projectBackend.GMotors.dto;

import java.math.BigDecimal;
import com.projectBackend.GMotors.model.DetalleFactura;
import java.util.List;
import java.util.stream.Collectors;

public class DetalleFacturaDTO {
    
    private Long idDetalleFactura;
    private String descripcion;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private Long idProducto;


    public DetalleFacturaDTO() {}

    public DetalleFacturaDTO(Long idDetalleFactura, String descripcion, 
                             Integer cantidad, BigDecimal precioUnitario, 
                             BigDecimal subtotal, Long idProducto) {
        this.idDetalleFactura = idDetalleFactura;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
        this.idProducto = idProducto;
    }

    // Mapeo desde entidad DetalleFactura
    public static DetalleFacturaDTO mapToDTO(DetalleFactura detalle) {
        BigDecimal precioUnitario = BigDecimal.ZERO;
        if (detalle.getCantidad() != null && detalle.getCantidad() > 0) {
            precioUnitario = detalle.getSubtotal()
                .divide(new BigDecimal(detalle.getCantidad()), 2, java.math.RoundingMode.HALF_UP);
        }

        return new DetalleFacturaDTO(
            detalle.getId_detalle(),
            detalle.getDescripcion(),
            detalle.getCantidad(),
            precioUnitario,
            detalle.getSubtotal(),
            detalle.getId_producto()
        );
    }

    // Mapeo de lista desde entidades DetalleFactura
    public static List<DetalleFacturaDTO> mapToDTOList(List<DetalleFactura> detalles) {
        return detalles.stream()
            .map(DetalleFacturaDTO::mapToDTO)
            .collect(Collectors.toList());
    }
    
    // ================= GETTERS & SETTERS =================

    public Long getIdDetalleFactura() {
        return idDetalleFactura;
    }

    public void setIdDetalleFactura(Long idDetalleFactura) {
        this.idDetalleFactura = idDetalleFactura;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }
}