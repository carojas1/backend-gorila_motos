package com.projectBackend.GMotors.repository;

import com.projectBackend.GMotors.model.CargaCombustible;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CargaCombustibleRepository extends JpaRepository<CargaCombustible, Long> {
    List<CargaCombustible> findByIdMoto(Long idMoto);
}
