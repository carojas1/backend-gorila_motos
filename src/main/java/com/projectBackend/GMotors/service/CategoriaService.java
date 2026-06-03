package com.projectBackend.GMotors.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.projectBackend.GMotors.model.Categoria;
import com.projectBackend.GMotors.repository.CategoriaRepository;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    // Crear categoría
    public Categoria crearCategoria(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    // Buscar categoría por ID
    public Optional<Categoria> buscarPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    // Listar todas las categorías
    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    // Actualizar categoría
    public Categoria actualizarCategoria(Long id, Categoria nuevaCategoria) {

        Categoria categoriaDB = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        categoriaDB.setNombre(nuevaCategoria.getNombre());
        categoriaDB.setDescripcion(nuevaCategoria.getDescripcion());

        return categoriaRepository.save(categoriaDB);
    }

    // Eliminar categoría
    public void eliminarCategoria(Long id) {
        categoriaRepository.deleteById(id);
    }
}
