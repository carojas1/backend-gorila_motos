package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.dto.DetalleFacturaCreateDTO;
import com.projectBackend.GMotors.model.DetalleFactura;
import com.projectBackend.GMotors.model.Factura;
import com.projectBackend.GMotors.model.Producto;
import com.projectBackend.GMotors.repository.ProductoRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DetalleFacturaService {

    private final ProductoRepository productoRepository;

    public DetalleFacturaService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    /**
     * Crea un DetalleFactura con subtotal calculado
     */
    public DetalleFactura crearDetalle(
            DetalleFacturaCreateDTO dto,
            Factura factura
    ) {

        validarCamposBase(dto);

        DetalleFactura detalle = new DetalleFactura();
        detalle.setIdFactura(factura.getIdFactura());
        detalle.setDescripcion(dto.getDescripcion());
        detalle.setCantidad(dto.getCantidad());

        BigDecimal subtotal;

        // 1️⃣ CASO PRODUCTO
        if (dto.getIdProducto() != null) {

            Producto producto = productoRepository.findById(dto.getIdProducto())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Backend-DetallesFactS: Producto no encontrado: " + dto.getIdProducto())
                    );

            BigDecimal precioUnitario = producto.getPvp();

            subtotal = precioUnitario
                    .multiply(BigDecimal.valueOf(dto.getCantidad()))
                    .setScale(2, RoundingMode.HALF_UP);

            detalle.setId_producto(producto.getId_producto());
            detalle.setDescripcion(producto.getNombre() != null && !producto.getNombre().isBlank()
                    ? producto.getNombre()
                    : producto.getDescripcion());
            
            // Restar el Stock del Producto
            int nuevoStock = producto.getStock() - dto.getCantidad();
            if (nuevoStock < 0) {
                throw new IllegalArgumentException(
                    "Stock insuficiente para el producto: " + producto.getDescripcion() + 
                    ". Stock disponible: " + producto.getStock()
                );
            }
            producto.setStock(nuevoStock);
            productoRepository.save(producto);

        }
        // 2️⃣ CASO SERVICIO
        else {

            if (dto.getPrecioUnitario() == null) {
                throw new IllegalArgumentException(
                        "Backend-DetallesFactS: PrecioUnitario es obligatorio para servicios"
                );
            }

            boolean esDescuentoPuntos = esDescuentoPuntos(dto.getDescripcion());
            if (dto.getPrecioUnitario().compareTo(BigDecimal.ZERO) < 0 && !esDescuentoPuntos) {
                throw new IllegalArgumentException(
                        "Backend-DetallesFactS: El precio no puede ser negativo"
                );
            }
            if (dto.getDescripcion() == null || dto.getDescripcion().isBlank()) {
                throw new IllegalArgumentException("Backend-DetallesFactS: La descripción es obligatoria");
            }
            if (esDescuentoPuntos && dto.getCantidad() != 1) {
                throw new IllegalArgumentException("Backend-DetallesFactS: El descuento de puntos debe tener cantidad 1");
            }
            //FIX:31/03/26
            //Atlas
            subtotal = dto.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(dto.getCantidad()))
                    .setScale(2, RoundingMode.HALF_UP);

            detalle.setId_producto(null);
        }

        detalle.setSubtotal(subtotal);
        return detalle;
    }



    // -------------------------------------------------------------------------
    //  Actualizar detalle
    // -------------------------------------------------------------------------
    public DetalleFactura actualizarDetalleConStock(
            DetalleFacturaCreateDTO dto,
            DetalleFactura detalleExistente
    ) {

        validarCamposBase(dto);

        Producto producto = null;
        if (dto.getIdProducto() != null) {
            producto = productoRepository.findById(dto.getIdProducto())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Producto no encontrado: " + dto.getIdProducto())
                    );
        }

        // Comparar cantidades
        int cantidadAnterior = detalleExistente.getCantidad();
        int cantidadNueva = dto.getCantidad();
        int diferencia = cantidadNueva - cantidadAnterior;

        // 2Ajustar stock SOLO si hay diferencia
        if (producto != null && diferencia != 0) {

            int nuevoStock = producto.getStock() - diferencia;

            if (nuevoStock < 0) {
                throw new IllegalArgumentException(
                        "Stock insuficiente al actualizar producto: " + producto.getDescripcion() +
                        ". Stock disponible actual: " + producto.getStock()
                );
            }

            producto.setStock(nuevoStock);
            productoRepository.save(producto);
        }

        // Actualizar el detalle en sí
        detalleExistente.setCantidad(cantidadNueva);

        if (producto != null) {
            detalleExistente.setId_producto(producto.getId_producto());
            detalleExistente.setDescripcion(producto.getNombre() != null && !producto.getNombre().isBlank()
                    ? producto.getNombre()
                    : producto.getDescripcion());
            detalleExistente.setSubtotal(
                    producto.getPvp()
                            .multiply(BigDecimal.valueOf(cantidadNueva))
                            .setScale(2, RoundingMode.HALF_UP)
            );
        } else {
            detalleExistente.setDescripcion(dto.getDescripcion());
            //FIX 31/03/2026 ATLAS
            detalleExistente.setSubtotal(
            	    dto.getPrecioUnitario()
            	        .multiply(BigDecimal.valueOf(cantidadNueva))
            	        .setScale(2, RoundingMode.HALF_UP)
            	);
        }

        return detalleExistente;
    }



    // ---------------- VALIDACIONES ----------------

    private void validarCamposBase(DetalleFacturaCreateDTO dto) {

        if (dto.getCantidad() == null || dto.getCantidad() <= 0) {
            throw new IllegalArgumentException("Backend-DetallesFactS: La cantidad debe ser mayor a 0");
        }
    }

    private boolean esDescuentoPuntos(String descripcion) {
        return descripcion != null && descripcion.trim().toUpperCase().startsWith("[DESC|PUNTOS:");
    }
}
