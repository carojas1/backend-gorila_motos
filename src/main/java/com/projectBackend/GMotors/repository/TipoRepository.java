package com.projectBackend.GMotors.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.projectBackend.GMotors.model.Tipo;
import java.util.List;
import java.util.Optional;

@Repository
public interface TipoRepository extends JpaRepository<Tipo, Long> {
    
    // Buscar tipo por nombre 
    Optional<Tipo> findByNombreIgnoreCase(String nombre);
    
    // Listar todos los tipos con producto
    @Query("SELECT t FROM Tipo t LEFT JOIN FETCH t.producto")
    List<Tipo> findAllWithProducto();
    
    // Buscar por nombre 
    @Query("SELECT t FROM Tipo t WHERE LOWER(t.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Tipo> buscarPorNombre(@Param("nombre") String nombre);
    
}
