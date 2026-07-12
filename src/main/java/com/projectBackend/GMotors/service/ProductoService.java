package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.model.Producto;
import com.projectBackend.GMotors.model.Factura;
import com.projectBackend.GMotors.dto.DetalleFacturaCreateDTO;
import com.projectBackend.GMotors.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

import java.util.List;
import java.util.Map;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;
    
    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @Autowired
    private FacturaService facturaService;

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
            if (producto.getCodigo_distribuidor() != null) {
                existing.setCodigo_distribuidor(producto.getCodigo_distribuidor());
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

    @Transactional
    public Factura registrarVentaDirecta(Long idProducto, Integer cantidad, Long idUsuario) {
        if (idProducto == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        if (idUsuario == null) {
            throw new IllegalArgumentException("El usuario que registra la venta es obligatorio");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }

        DetalleFacturaCreateDTO detalle = new DetalleFacturaCreateDTO();
        detalle.setIdProducto(idProducto);
        detalle.setCantidad(cantidad);

        Factura factura = facturaService.crearFactura(List.of(detalle), idUsuario);
        productoRepository.findById(idProducto).ifPresent(producto -> {
            producto.setFecha_modificacion(LocalDate.now());
            productoRepository.save(producto);
        });
        return factura;
    }

    @Transactional
    public Factura registrarVentaDirecta(
            List<DetalleFacturaCreateDTO> detalles,
            Long idUsuario,
            Map<String, Object> cliente
    ) {
        if (idUsuario == null) {
            throw new IllegalArgumentException("El usuario que registra la venta es obligatorio");
        }
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La venta debe tener al menos un producto");
        }

        Factura factura = facturaService.crearFactura(detalles, idUsuario);
        factura.setOrigenVenta("INVENTARIO");
        if (cliente != null) {
            factura.setClienteNombre(text(cliente, "nombre", "nombreCliente"));
            factura.setClienteCedula(text(cliente, "cedula", "ruc", "identificacion"));
            factura.setClienteTelefono(text(cliente, "telefono", "celular"));
            factura.setClienteCorreo(text(cliente, "correo", "email"));
            factura.setClienteDireccion(text(cliente, "direccion"));
            factura.setClienteTipo(text(cliente, "tipo"));
        }
        return facturaService.save(factura);
    }

    private String text(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return null;
    }


}
