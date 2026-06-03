package com.projectBackend.GMotors.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectBackend.GMotors.model.Moto;
import com.projectBackend.GMotors.repository.MotoRepository;
import com.projectBackend.GMotors.repository.UsuarioRepository;

@Service
public class MotoService {

    @Autowired
    private MotoRepository motoRepository;
    
    @Autowired
    private SupabaseStorageService supabaseStorageService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ✅ Crear moto (CON validación de usuario)
    @Transactional
    public Moto crearMoto(Moto moto) {
        // Validar que el usuario exista antes de crear la moto
        if (!usuarioRepository.existsById(moto.getId_usuario())) {
            throw new RuntimeException("Usuario no encontrado con ID: " + moto.getId_usuario());
        }
        return motoRepository.save(moto);
    }

    // ✅ Buscar moto por ID
    public Optional<Moto> buscarPorId(Long id) {
        return motoRepository.findById(id);
    }

	// Listar todas las motos
	public List<Moto> listarTodas() {
		return motoRepository.findAll();
	}
	
    // ✅ Listar motos de un usuario
    public List<Moto> listarPorUsuario(Long idUsuario) {
        return motoRepository.findByIdUsuario(idUsuario);
    }
    
 // Buscar usuario por Placa
    public Optional<Moto> buscarPorPlaca(String placa) {
        return motoRepository.findByPlacaIgnoreCase(placa);
    }


	// Actualizar moto
    @Transactional
	public Moto actualizarMoto(Long id, Moto motoActualizada) {
	    Moto motoDB = motoRepository.findById(id)
	        .orElseThrow(() -> new RuntimeException("Moto no encontrada con ID: " + id));

	    if (motoActualizada.getPlaca() != null) {
	        motoDB.setPlaca(motoActualizada.getPlaca());
	    }
	    if (motoActualizada.getAnio() != null) {
	        motoDB.setAnio(motoActualizada.getAnio());
	    }
	    if (motoActualizada.getMarca() != null) {
	        motoDB.setMarca(motoActualizada.getMarca());
	    }
	    if (motoActualizada.getModelo() != null) {
	        motoDB.setModelo(motoActualizada.getModelo());
	    }   
	    if (motoActualizada.getNombreMoto() != null) {
		        motoDB.setNombreMoto(motoActualizada.getNombreMoto());
	    }
	    if (motoActualizada.getKilometraje() != null) {
	        motoDB.setKilometraje(motoActualizada.getKilometraje());
	    }
	    if (motoActualizada.getCilindraje() != null) {
	        motoDB.setCilindraje(motoActualizada.getCilindraje());
	    }
	    if (motoActualizada.getTipoMoto() != null) {
	        motoDB.setTipoMoto(motoActualizada.getTipoMoto()); 
	    }
	    if (motoActualizada.getRuta_imagenMotos() != null) {
	        // Si la imagen cambió, eliminar la anterior
	        if (motoDB.getRuta_imagenMotos() != null &&
	            !motoDB.getRuta_imagenMotos().equals(motoActualizada.getRuta_imagenMotos())) {
	            
	            try {
	                supabaseStorageService.eliminarImagen(motoDB.getRuta_imagenMotos());
	            } catch (Exception e) {
	                System.err.println("Advertencia: No se pudo eliminar imagen anterior: " + e.getMessage());
	                // No interrumpir el flujo si falla la eliminación
	            }
	        }
	        motoDB.setRuta_imagenMotos(motoActualizada.getRuta_imagenMotos());
	    }
	    if (motoActualizada.getId_usuario() != null) {
	        motoDB.setId_usuario(motoActualizada.getId_usuario());
	    }

	    return motoRepository.save(motoDB);
	}


    // ✅ Eliminar moto
    @Transactional
    public void eliminarMoto(Long id) {
        if (!motoRepository.existsById(id)) {
            throw new RuntimeException("Moto no encontrada con ID: " + id);
        }
        motoRepository.deleteById(id);
    }
}