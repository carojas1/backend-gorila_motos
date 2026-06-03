package com.projectBackend.GMotors.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.projectBackend.GMotors.model.Rol;
import com.projectBackend.GMotors.repository.RolRepository;

@Service
public class RolService {

    @Autowired
    private RolRepository rolRepository;

    // Crear rol
    public Rol crearRol(Rol rol) {
        return rolRepository.save(rol);
    }

    // Buscar rol por ID
    public Optional<Rol> buscarPorId(Long id) {
        return rolRepository.findById(id);
    }

    // Listar todos los roles
    public List<Rol> listarTodos() {
        return rolRepository.findAll();
    }

    // Actualizar rol
    public Rol actualizarRol(Long id, Rol rolActualizado) {

        Rol rolDB = rolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        rolDB.setNombre(rolActualizado.getNombre());

        return rolRepository.save(rolDB);
    }

    // Eliminar rol
    public void eliminarRol(Long id) {
        rolRepository.deleteById(id);
    }
}
