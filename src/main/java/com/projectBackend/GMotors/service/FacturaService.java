package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.dto.DetalleFacturaCreateDTO;
import com.projectBackend.GMotors.dto.DetalleFacturaDTO;
import com.projectBackend.GMotors.model.DetalleFactura;
import com.projectBackend.GMotors.model.Factura;
import com.projectBackend.GMotors.model.Producto;
import com.projectBackend.GMotors.repository.DetalleFacturaRepository;
import com.projectBackend.GMotors.repository.FacturaRepository;
import com.projectBackend.GMotors.repository.ProductoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final DetalleFacturaRepository detalleFacturaRepository;
    private final DetalleFacturaService detalleFacturaService;
    private final ProductoRepository productoRepository;

    public FacturaService(
            FacturaRepository facturaRepository,
            DetalleFacturaRepository detalleFacturaRepository,
            DetalleFacturaService detalleFacturaService,
            ProductoRepository productoRepository
    ) {
        this.facturaRepository = facturaRepository;
        this.detalleFacturaRepository = detalleFacturaRepository;
        this.detalleFacturaService = detalleFacturaService;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public List<Factura> listarTodas() {
        List<Factura> facturas = facturaRepository.findAll();
        facturas.forEach(this::sincronizarTotalConDetalles);
        return facturas;
    }

    public Factura save(Factura factura) {
        return facturaRepository.save(factura);
    }

    @Transactional
    public DetalleFactura saveDetalle(DetalleFactura detalle) {
        DetalleFactura guardado = detalleFacturaRepository.save(detalle);
        facturaRepository.findById(guardado.getIdFactura()).ifPresent(this::sincronizarTotalConDetalles);
        return guardado;
    }

    // =====================================================================
    // CREAR FACTURA
    // =====================================================================
    @Transactional
    public Factura crearFactura(
            List<DetalleFacturaCreateDTO> detallesDTO,
            Long idUsuarioCliente
    ) {

        if (detallesDTO == null || detallesDTO.isEmpty()) {
            throw new IllegalArgumentException(
                    "La factura debe contener al menos un detalle"
            );
        }

        // 1️⃣ Crear factura base
        Factura factura = new Factura();
        factura.setCostoTotal(BigDecimal.ZERO);
        factura.setFechaEmision(LocalDate.now());
        factura.setIdUsuario(idUsuarioCliente);
        factura = facturaRepository.save(factura);

        BigDecimal totalFactura = BigDecimal.ZERO;

        // 2️⃣ Crear y guardar detalles
        for (DetalleFacturaCreateDTO dto : detallesDTO) {
            DetalleFactura detalle = detalleFacturaService.crearDetalle(dto, factura);
            detalleFacturaRepository.save(detalle);
            totalFactura = totalFactura.add(detalle.getSubtotal());
        }

        totalFactura = normalizarDinero(totalFactura);
        validarTotalNoNegativo(totalFactura);
        factura.setCostoTotal(totalFactura);
        return facturaRepository.save(factura);
    }

    // =====================================================================
    // ACTUALIZAR FACTURA CON CONTROL TOTAL DE STOCK
    // =====================================================================
    @Transactional
    public Factura actualizarFactura(
            Long idFactura,
            List<DetalleFacturaCreateDTO> nuevosDTO
    ) {
        Factura factura = facturaRepository.findById(idFactura)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Factura no encontrada con ID: " + idFactura));

        if (nuevosDTO == null || nuevosDTO.isEmpty()) {
            throw new IllegalArgumentException("La factura debe contener al menos un detalle");
        }

        // Obtener detalles actuales
        List<DetalleFactura> detallesActuales = detalleFacturaRepository.findByIdFactura(idFactura);
        for (DetalleFactura d : detallesActuales) {
            devolverStock(d);
        }
        detalleFacturaRepository.deleteAll(detallesActuales);
        detalleFacturaRepository.flush();

        // Indexar solo los detalles de PRODUCTOS (tienen id_producto)
        BigDecimal total = BigDecimal.ZERO;

        // Procesar los NUEVOS detalles
        for (DetalleFacturaCreateDTO dto : nuevosDTO) {
            DetalleFactura nuevo = detalleFacturaService.crearDetalle(dto, factura);
            detalleFacturaRepository.save(nuevo);
            total = total.add(nuevo.getSubtotal());
        }

        total = normalizarDinero(total);
        validarTotalNoNegativo(total);
        // Actualizar total
        factura.setCostoTotal(total);
        return facturaRepository.save(factura);
    }

    // =====================================================================
    // DEVOLVER STOCK DE UN DETALLE ELIMINADO
    // =====================================================================
    private void devolverStock(DetalleFactura detalle) {

        if (detalle.getId_producto() == null) return; // era servicio

        Producto producto = productoRepository.findById(detalle.getId_producto())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado al devolver stock"));

        producto.setStock(producto.getStock() + detalle.getCantidad());
        productoRepository.save(producto);
    }

    private void validarTotalNoNegativo(BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El descuento no puede dejar la factura en negativo");
        }
    }

    private BigDecimal normalizarDinero(BigDecimal valor) {
        return (valor != null ? valor : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    // =====================================================================
    // Obtener factura y detalles
    // =====================================================================

    public List<DetalleFacturaDTO> obtenerDetallesPorFactura(Long idFactura) {
        List<DetalleFactura> detalles = detalleFacturaRepository.findByIdFactura(idFactura);
        return DetalleFacturaDTO.mapToDTOList(detalles);
    }

    @Transactional
    public Optional<Factura> obtenerFacturaPorId(Long idFactura) {
        return facturaRepository.findById(idFactura).map(this::sincronizarTotalConDetalles);
    }

    @Transactional
    public List<Factura> obtenerFacturasPorUsuario(Long idUsuario) {
        List<Factura> facturas = facturaRepository.findByIdUsuario(idUsuario);
        facturas.forEach(this::sincronizarTotalConDetalles);
        return facturas;
    }

    public BigDecimal calcularTotalDetalles(Long idFactura) {
        return detalleFacturaRepository.findByIdFactura(idFactura).stream()
                .map(d -> d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Factura sincronizarTotalConDetalles(Factura factura) {
        List<DetalleFactura> detalles = detalleFacturaRepository.findByIdFactura(factura.getIdFactura());
        if (detalles.isEmpty()) return factura;

        BigDecimal totalReal = detalles.stream()
                .map(d -> d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (factura.getCostoTotal() == null || factura.getCostoTotal().compareTo(totalReal) != 0) {
            factura.setCostoTotal(totalReal);
            return facturaRepository.save(factura);
        }
        return factura;
    }

    // =====================================================================
    // ELIMINAR FACTURA COMPLETA (Devolver stock)
    // =====================================================================
    @Transactional
    public void eliminarFactura(Long idFactura) {

        if (!facturaRepository.existsById(idFactura)) {
            throw new IllegalArgumentException("Factura no encontrada con ID: " + idFactura);
        }

        List<DetalleFactura> detalles = detalleFacturaRepository.findByIdFactura(idFactura);

        // devolver stock
        for (DetalleFactura d : detalles) {
            devolverStock(d);
        }

        detalleFacturaRepository.deleteByIdFactura(idFactura);
        facturaRepository.deleteById(idFactura);
    }
}
