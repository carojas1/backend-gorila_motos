package com.projectBackend.GMotors.repository;

import com.projectBackend.GMotors.model.AlertaEnviada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaEnviadaRepository extends JpaRepository<AlertaEnviada, Long> {

    boolean existsByIdMotoAndTipoAndKmUmbral(Long idMoto, String tipo, Integer kmUmbral);
}
