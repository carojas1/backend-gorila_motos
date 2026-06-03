package com.projectBackend.GMotors.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.projectBackend.GMotors.model.Categoria;
import com.projectBackend.GMotors.service.CategoriaService;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin("*")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    // Crear categoría
    @PostMapping
    public Categoria crearCategoria(@RequestBody Categoria categoria) {
        return categoriaService.crearCategoria(categoria);
    }

    // Obtener categoría por ID
    @GetMapping("/{id}")
    public Categoria obtenerPorId(@PathVariable Long id) {
        return categoriaService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
    }


    // Listar todas las categorías
    @GetMapping
    public List<Categoria> listarTodas() {
        return categoriaService.listarTodas();
    }

    // Actualizar categoría
    @PutMapping("/{id}")
    public Categoria actualizarCategoria(
            @PathVariable Long id,
            @RequestBody Categoria categoriaActualizada) {
        return categoriaService.actualizarCategoria(id, categoriaActualizada);
    }

    // Eliminar categoría
    @DeleteMapping("/{id}")
    public String eliminarCategoria(@PathVariable Long id) {
        categoriaService.eliminarCategoria(id);
        return "Categoría eliminada correctamente";
    }
}
