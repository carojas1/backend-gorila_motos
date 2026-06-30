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

    public List<Factura> listarTodas() {
        return facturaRepository.findAll();
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
            if (d.getId_producto() == null) {
                detalleFacturaRepository.delete(d);
            }
        }

        // Indexar solo los detalles de PRODUCTOS (tienen id_producto)
        Map<Long, DetalleFactura> mapaActuales = new HashMap<>();
        for (DetalleFactura d : detallesActuales) {
            if (d.getId_producto() != null) {
                mapaActuales.put(d.getId_producto(), d);
            }
        }

        BigDecimal total = BigDecimal.ZERO;

        // Procesar los NUEVOS detalles
        for (DetalleFacturaCreateDTO dto : nuevosDTO) {

            if (dto.getIdProducto() != null && mapaActuales.containsKey(dto.getIdProducto())) {
                //Producto existente → actualizar cantidad y stock
                DetalleFactura existente = mapaActuales.get(dto.getIdProducto());
                detalleFacturaService.actualizarDetalleConStock(dto, existente);
                detalleFacturaRepository.save(existente);
                total = total.add(existente.getSubtotal());
                mapaActuales.remove(dto.getIdProducto());

            } else {
                // Producto nuevo O servicio → crear
                DetalleFactura nuevo = detalleFacturaService.crearDetalle(dto, factura);
                detalleFacturaRepository.save(nuevo);
                total = total.add(nuevo.getSubtotal());
            }
        }

        // 3Productos que quedaron en el mapa → fueron eliminados → devolver stock
        for (DetalleFactura eliminado : mapaActuales.values()) {
            devolverStock(eliminado);
            detalleFacturaRepository.delete(eliminado);
        }

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

    // =====================================================================
    // Obtener factura y detalles
    // =====================================================================

    public List<DetalleFacturaDTO> obtenerDetallesPorFactura(Long idFactura) {
        List<DetalleFactura> detalles = detalleFacturaRepository.findByIdFactura(idFactura);
        return DetalleFacturaDTO.mapToDTOList(detalles);
    }

    public Optional<Factura> obtenerFacturaPorId(Long idFactura) {
        return facturaRepository.findById(idFactura);
    }

    public List<Factura> obtenerFacturasPorUsuario(Long idUsuario) {
        return facturaRepository.findByIdUsuario(idUsuario);
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