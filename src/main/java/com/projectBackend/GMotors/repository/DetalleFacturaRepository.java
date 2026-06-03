package com.projectBackend.GMotors.repository;

import com.projectBackend.GMotors.model.DetalleFactura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleFacturaRepository
        extends JpaRepository<DetalleFactura, Long> {

    List<DetalleFactura> findByIdFactura(Long idFactura);

    void deleteByIdFactura(Long idFactura);

    boolean existsByIdFactura(Long idFactura);
}
