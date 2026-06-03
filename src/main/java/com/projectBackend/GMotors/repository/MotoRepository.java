package com.projectBackend.GMotors.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projectBackend.GMotors.model.Moto;
import java.util.List;

public interface MotoRepository extends JpaRepository<Moto, Long> {
    List<Moto> findByIdUsuario(Long idUsuario);
    
    Optional<Moto> findByPlacaIgnoreCase(String placa);
}