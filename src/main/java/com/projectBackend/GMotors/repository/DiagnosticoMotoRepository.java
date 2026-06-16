package com.projectBackend.GMotors.repository;

import com.projectBackend.GMotors.model.DiagnosticoMoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiagnosticoMotoRepository extends JpaRepository<DiagnosticoMoto, Long> {

    List<DiagnosticoMoto> findByIdMotoOrderByFechaDesc(Long idMoto);

    List<DiagnosticoMoto> findByIdMecanicoOrderByFechaDesc(Long idMecanico);
}
