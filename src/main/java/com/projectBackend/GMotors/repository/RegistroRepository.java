package com.projectBackend.GMotors.repository;

import com.projectBackend.GMotors.model.Registro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistroRepository extends JpaRepository<Registro, Long> {

    List<Registro> findByCliente_IdUsuario(Long idCliente);

    List<Registro> findByEncargado_IdUsuario(Long idEncargado);

    List<Registro> findByEstado(Integer estado);
    
    List<Registro> findByCliente_IdUsuarioOrderByFechaDesc(Long idCliente);
    
    @Query("SELECT r FROM Registro r WHERE LOWER(r.cliente.nombre_completo) LIKE LOWER(CONCAT('%', :nombreCliente, '%')) ORDER BY r.fecha DESC")
    List<Registro> buscarPorNombreCliente(@Param("nombreCliente") String nombreCliente);
    
    List<Registro> findByMoto_Placa(String placa);
    
    List<Registro> findByMoto_PlacaContainingIgnoreCase(String placa);
}

