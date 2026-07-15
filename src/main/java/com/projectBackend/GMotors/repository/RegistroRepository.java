package com.projectBackend.GMotors.repository;

import com.projectBackend.GMotors.model.Registro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistroRepository extends JpaRepository<Registro, Long> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "encargado", "moto", "tipo", "factura"})
    List<Registro> findAll();

    @EntityGraph(attributePaths = {"cliente", "encargado", "moto", "tipo", "factura"})
    List<Registro> findByCliente_IdUsuario(Long idCliente);

    @EntityGraph(attributePaths = {"cliente", "encargado", "moto", "tipo", "factura"})
    List<Registro> findByEncargado_IdUsuario(Long idEncargado);

    List<Registro> findByEstado(Integer estado);
    
    @EntityGraph(attributePaths = {"cliente", "encargado", "moto", "tipo", "factura"})
    List<Registro> findByCliente_IdUsuarioOrderByFechaDesc(Long idCliente);
    
    @Query("SELECT r FROM Registro r WHERE LOWER(r.cliente.nombre_completo) LIKE LOWER(CONCAT('%', :nombreCliente, '%')) ORDER BY r.fecha DESC")
    @EntityGraph(attributePaths = {"cliente", "encargado", "moto", "tipo", "factura"})
    List<Registro> buscarPorNombreCliente(@Param("nombreCliente") String nombreCliente);
    
    List<Registro> findByMoto_Placa(String placa);
    
    @EntityGraph(attributePaths = {"cliente", "encargado", "moto", "tipo", "factura"})
    List<Registro> findByMoto_PlacaContainingIgnoreCase(String placa);
}

