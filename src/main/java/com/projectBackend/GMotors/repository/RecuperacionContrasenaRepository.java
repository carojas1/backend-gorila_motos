package com.projectBackend.GMotors.repository;

import com.projectBackend.GMotors.model.RecuperacionContrasena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RecuperacionContrasenaRepository extends JpaRepository<RecuperacionContrasena, Long> {
    Optional<RecuperacionContrasena> findByToken(String token);
}