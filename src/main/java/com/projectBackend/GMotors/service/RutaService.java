package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.model.Ruta;
import com.projectBackend.GMotors.repository.RutaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RutaService {

    @Autowired
    private RutaRepository rutaRepository;

    // Crear ruta
    public Ruta crearRuta(Ruta ruta) {
        return rutaRepository.save(ruta);
    }

     // Listar rutas del usuario
    public List<Ruta> listarRutasPorUsuario(Long idUsuario) {
        return rutaRepository.findByIdUsuarioOrderByIdRutaDesc(idUsuario);
    }

    // Obtener ruta por ID
    public Optional<Ruta> obtenerRutaPorId(Long idRuta) {
        return rutaRepository.findById(idRuta);
    }

    // Actualizar ruta
    public Ruta actualizarRuta(Long idRuta, Ruta rutaActualizada) {
        Ruta rutaExistente = rutaRepository.findById(idRuta)
                .orElseThrow(() -> new RuntimeException("Ruta no encontrada"));

        if (rutaActualizada.getNombreRuta() != null) {
            rutaExistente.setNombreRuta(rutaActualizada.getNombreRuta());
        }
        if (rutaActualizada.getDescripcion() != null) {
            rutaExistente.setDescripcion(rutaActualizada.getDescripcion());
        }

        return rutaRepository.save(rutaExistente);
    }

    // Eliminar ruta
    public void eliminarRuta(Long idRuta) {
        if (!rutaRepository.existsById(idRuta)) {
            throw new RuntimeException("Ruta no encontrada");
        }
        rutaRepository.deleteById(idRuta);
    }
}