package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.model.Producto;
import com.projectBackend.GMotors.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

import java.util.List;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;
    
    @Autowired
    private SupabaseStorageService supabaseStorageService;

    // Obtener todos los productos
    public List<Producto> getAllProductos() {
        return productoRepository.findAll();
    }

    // Obtener producto por ID
    public Producto getProductoById(Long id) {
        return productoRepository.findById(id).orElse(null);
    }

    // Crear producto
    public Producto createProducto(Producto producto) {
    	
    	producto.setFecha_registro(LocalDate.now());
        producto.setFecha_modificacion(LocalDate.now());
        
        return productoRepository.save(producto);
    }

    // Actualizar producto
    public Producto updateProducto(Long id, Producto producto) {
        Producto existing = productoRepository.findById(id).orElse(null);

        if (existing != null) {
            if (producto.getNombre() != null) {
                existing.setNombre(producto.getNombre());
            }
            if (producto.getCosto() != null) {
                existing.setCosto(producto.getCosto());
            }
            if (producto.getDescripcion() != null) {
                existing.setDescripcion(producto.getDescripcion());
            }
            if (producto.getId_categoria() != null) {
                existing.setId_categoria(producto.getId_categoria());
            }
            if (producto.getStock() != null) {
                existing.setStock(producto.getStock());
            }
            if (producto.getCodigo_personal() != null) {
                existing.setCodigo_personal(producto.getCodigo_personal());
            }
            if (producto.getCodigo_proveedor() != null) {
                existing.setCodigo_proveedor(producto.getCodigo_proveedor());
            }
            
            String nuevaFoto = producto.getruta_imagenproductos();
            if (nuevaFoto != null && !nuevaFoto.isBlank()) {
                String anterior = existing.getruta_imagenproductos();
                if (anterior != null && anterior.startsWith("http") && !anterior.equals(nuevaFoto)) {
                    try {
                        supabaseStorageService.eliminarImagen(anterior);
                    } catch (Exception e) {
                        // Ignorar si no se puede eliminar la anterior
                    }
                }
                existing.setruta_imagenproductos(nuevaFoto);
            }
            
            if (producto.getPvp() != null) {
                existing.setPvp(producto.getPvp());
            }

            // IMPORTANTE:
            existing.setFecha_modificacion(LocalDate.now());

            return productoRepository.save(existing);
        }
        return null;
    }

    // Eliminar producto por ID
    public boolean deleteProducto(Long id) {
        if (productoRepository.existsById(id)) {
            productoRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    // Eliminar varios productos 
    public void deleteProductos(List<Long> ids) {
        productoRepository.deleteAllById(ids);
    }


}
