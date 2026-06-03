package com.projectBackend.GMotors.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projectBackend.GMotors.model.Rol;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

}

