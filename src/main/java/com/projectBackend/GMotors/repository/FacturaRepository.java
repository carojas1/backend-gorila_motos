package com.projectBackend.GMotors.repository;

import com.projectBackend.GMotors.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    // Obtener facturas por usuario
    List<Factura> findByIdUsuario(Long idUsuario);

    // Obtener facturas por fecha
    List<Factura> findByFechaEmision(LocalDate fechaEmision);

    // Verificar si existe una factura
    boolean existsByIdFactura(Long idFactura);
}
