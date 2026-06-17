package com.projectBackend.GMotors.repository;

import com.projectBackend.GMotors.model.MantenimientoRealizado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MantenimientoRealizadoRepository extends JpaRepository<MantenimientoRealizado, Long> {
    List<MantenimientoRealizado> findByIdMoto(Long idMoto);
    List<MantenimientoRealizado> findByIdMotoAndTipo(Long idMoto, String tipo);
}
