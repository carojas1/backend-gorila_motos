package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.model.Tipo;
import com.projectBackend.GMotors.model.Producto;
import com.projectBackend.GMotors.repository.TipoRepository;
import com.projectBackend.GMotors.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TipoService {
    @Autowired
    private TipoRepository tipoRepository;
    
    @Autowired
    private ProductoRepository productoRepository; 

    // Crear un tipo
    public Tipo crearTipo(Tipo tipo) {
        if (tipo.getId_producto() != null) {
            Producto producto = productoRepository.findById(tipo.getId_producto())
                    .orElse(null);
            tipo.setProducto(producto);
        }
        return tipoRepository.save(tipo);
    }

    // Obtener todos los tipos
    public List<Tipo> obtenerTodos() {
        return tipoRepository.findAll();
    }

    // Buscar tipo por ID
    public Optional<Tipo> obtenerPorId(Long id) {
        return tipoRepository.findById(id);
    }

    // Actualizar tipo
    public Tipo actualizarTipo(Long id, Tipo tipoActualizado) {
        Tipo tipoDB = tipoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo no encontrado"));
        
        tipoDB.setNombre(tipoActualizado.getNombre());
        tipoDB.setDescripcion(tipoActualizado.getDescripcion());
        tipoDB.setConcepto_manual(tipoActualizado.getConcepto_manual());
        tipoDB.setConcepto_cantidad(tipoActualizado.getConcepto_cantidad());
        tipoDB.setConcepto_precio_unitario(tipoActualizado.getConcepto_precio_unitario());
        
        // ← BUSCAR PRODUCTO POR ID
        if (tipoActualizado.getId_producto() != null) {
            Producto producto = productoRepository.findById(tipoActualizado.getId_producto())
                    .orElse(null);
            tipoDB.setProducto(producto);
        } else {
            tipoDB.setProducto(null);
        }
        
        return tipoRepository.save(tipoDB);
    }

    // Eliminar tipo
    public void eliminarTipo(Long id) {
        tipoRepository.deleteById(id);
    }
}