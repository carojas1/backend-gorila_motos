package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.dto.DetalleFacturaCreateDTO;
import com.projectBackend.GMotors.model.DetalleFactura;
import com.projectBackend.GMotors.model.Factura;
import com.projectBackend.GMotors.repository.DetalleFacturaRepository;
import com.projectBackend.GMotors.repository.FacturaRepository;
import com.projectBackend.GMotors.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private DetalleFacturaRepository detalleFacturaRepository;
    @Mock
    private DetalleFacturaService detalleFacturaService;
    @Mock
    private ProductoRepository productoRepository;

    private FacturaService facturaService;

    @BeforeEach
    void setUp() {
        facturaService = new FacturaService(
                facturaRepository,
                detalleFacturaRepository,
                detalleFacturaService,
                productoRepository
        );
        when(facturaRepository.save(any(Factura.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void listarTodasCorrigeUnTotalDesincronizado() {
        Factura factura = factura(9L, "0.48");
        List<DetalleFactura> detalles = List.of(
                detalle(9L, "12.00"),
                detalle(9L, "15.00")
        );
        when(facturaRepository.findAll()).thenReturn(List.of(factura));
        when(detalleFacturaRepository.findByIdFactura(9L)).thenReturn(detalles);

        List<Factura> resultado = facturaService.listarTodas();

        assertEquals(new BigDecimal("27.00"), resultado.get(0).getCostoTotal());
        verify(facturaRepository).save(factura);
    }

    @Test
    void actualizarFacturaReemplazaTodosLosDetallesYRecalculaElTotal() {
        Factura factura = factura(12L, "99.00");
        List<DetalleFactura> anteriores = List.of(
                detalle(12L, "20.00"),
                detalle(12L, "79.00")
        );
        DetalleFacturaCreateDTO primero = new DetalleFacturaCreateDTO();
        primero.setCantidad(1);
        DetalleFacturaCreateDTO segundo = new DetalleFacturaCreateDTO();
        segundo.setCantidad(1);
        DetalleFactura nuevoPrimero = detalle(12L, "12.00");
        DetalleFactura nuevoSegundo = detalle(12L, "15.00");

        when(facturaRepository.findById(12L)).thenReturn(Optional.of(factura));
        when(detalleFacturaRepository.findByIdFactura(12L)).thenReturn(anteriores);
        when(detalleFacturaService.crearDetalle(primero, factura)).thenReturn(nuevoPrimero);
        when(detalleFacturaService.crearDetalle(segundo, factura)).thenReturn(nuevoSegundo);

        Factura resultado = facturaService.actualizarFactura(12L, List.of(primero, segundo));

        assertEquals(new BigDecimal("27.00"), resultado.getCostoTotal());
        verify(detalleFacturaRepository).deleteAll(anteriores);
        verify(detalleFacturaRepository).flush();
        verify(detalleFacturaRepository, times(2)).save(any(DetalleFactura.class));
    }

    private Factura factura(Long id, String total) {
        Factura factura = new Factura();
        factura.setIdFactura(id);
        factura.setCostoTotal(new BigDecimal(total));
        return factura;
    }

    private DetalleFactura detalle(Long idFactura, String subtotal) {
        DetalleFactura detalle = new DetalleFactura();
        detalle.setIdFactura(idFactura);
        detalle.setCantidad(1);
        detalle.setSubtotal(new BigDecimal(subtotal));
        return detalle;
    }
}
