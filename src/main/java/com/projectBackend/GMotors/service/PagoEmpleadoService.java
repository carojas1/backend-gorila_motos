package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.model.PagoEmpleado;
import com.projectBackend.GMotors.repository.PagoEmpleadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PagoEmpleadoService {

    @Autowired
    private PagoEmpleadoRepository repo;

    public List<PagoEmpleado> listarPorEmpleado(Long idEmpleado) {
        return repo.findByIdEmpleadoOrderByFechaDesc(idEmpleado);
    }

    public List<PagoEmpleado> listarTodos() {
        return repo.findAllByOrderByFechaDesc();
    }

    public PagoEmpleado crear(PagoEmpleado pago) {
        return repo.save(pago);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}
