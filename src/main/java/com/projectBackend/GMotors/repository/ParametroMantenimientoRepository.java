package com.projectBackend.GMotors.repository;

import com.projectBackend.GMotors.model.ParametroMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ParametroMantenimientoRepository extends JpaRepository<ParametroMantenimiento, Long> {

    @Query("SELECT p FROM ParametroMantenimiento p WHERE p.ccMin <= :cc AND (p.ccMax IS NULL OR p.ccMax >= :cc)")
    List<ParametroMantenimiento> findByCc(@Param("cc") int cc);
}
