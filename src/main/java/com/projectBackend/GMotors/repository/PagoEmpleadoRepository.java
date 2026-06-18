package com.projectBackend.GMotors.repository;

import com.projectBackend.GMotors.model.PagoEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagoEmpleadoRepository extends JpaRepository<PagoEmpleado, Long> {
    List<PagoEmpleado> findByIdEmpleadoOrderByFechaDesc(Long idEmpleado);
    List<PagoEmpleado> findAllByOrderByFechaDesc();
}
