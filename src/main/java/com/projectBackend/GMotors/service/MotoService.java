package com.projectBackend.GMotors.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectBackend.GMotors.model.MantenimientoRealizado;
import com.projectBackend.GMotors.model.Moto;
import com.projectBackend.GMotors.model.ParametroMantenimiento;
import com.projectBackend.GMotors.repository.MantenimientoRealizadoRepository;
import com.projectBackend.GMotors.repository.MotoRepository;
import com.projectBackend.GMotors.repository.ParametroMantenimientoRepository;
import com.projectBackend.GMotors.repository.UsuarioRepository;
import java.time.LocalDate;

@Service
public class MotoService {

    @Autowired
    private MotoRepository motoRepository;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AlertaMantenimientoService alertaService;

    @Autowired
    private MantenimientoRealizadoRepository mantenimientoRealizadoRepository;

    @Autowired
    private ParametroMantenimientoRepository parametroMantenimientoRepository;

    // ✅ Crear moto (CON validación de usuario e inicialización de mantenimientos)
    @Transactional
    public Moto crearMoto(Moto moto) {
        // Validar que el usuario exista antes de crear la moto
        if (!usuarioRepository.existsById(moto.getId_usuario())) {
            throw new RuntimeException("Usuario no encontrado con ID: " + moto.getId_usuario());
        }
        Moto nuevaMoto = motoRepository.save(moto);

        // Inicializar mantenimientos base (0% desgaste al kilometraje actual)
        if (nuevaMoto.getCilindraje() != null && nuevaMoto.getKilometraje() != null) {
            List<ParametroMantenimiento> parametros = parametroMantenimientoRepository.findByCc(nuevaMoto.getCilindraje());
            for (ParametroMantenimiento p : parametros) {
                MantenimientoRealizado inicial = new MantenimientoRealizado();
                inicial.setIdMoto(nuevaMoto.getIdMoto());
                inicial.setTipo(p.getTipoMantenimiento());
                // Extrapolar el último cambio teórico basado en el intervalo para no empezar desde 0% de desgaste irreal
                int kmServicio = nuevaMoto.getKilometraje();
                if (p.getIntervaloKm() != null && p.getIntervaloKm() > 0) {
                    kmServicio = (nuevaMoto.getKilometraje() / p.getIntervaloKm()) * p.getIntervaloKm();
                }
                inicial.setKmServicio(kmServicio);
                inicial.setFecha(LocalDate.now());
                mantenimientoRealizadoRepository.save(inicial);
            }
        }

        return nuevaMoto;
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
	    boolean kmActualizado = false;
	    if (motoActualizada.getKilometraje() != null) {
	        if (!motoActualizada.getKilometraje().equals(motoDB.getKilometraje())) {
	            kmActualizado = true;
	        }
	        motoDB.setKilometraje(motoActualizada.getKilometraje());
	    }
	    if (motoActualizada.getCilindraje() != null) {
	        motoDB.setCilindraje(motoActualizada.getCilindraje());
	    }
	    if (motoActualizada.getTipoMoto() != null) {
	        motoDB.setTipoMoto(motoActualizada.getTipoMoto()); 
	    }
	    String nuevaFoto = motoActualizada.getRuta_imagenMotos();
	    if (nuevaFoto != null && !nuevaFoto.equals("Desconocido") && !nuevaFoto.isBlank()) {
	        String anterior = motoDB.getRuta_imagenMotos();
	        if (anterior != null && anterior.startsWith("http") && !anterior.equals(nuevaFoto)) {
	            try {
	                supabaseStorageService.eliminarImagen(anterior);
	            } catch (Exception e) {
	                System.err.println("Advertencia: No se pudo eliminar imagen anterior: " + e.getMessage());
	            }
	        }
	        motoDB.setRuta_imagenMotos(nuevaFoto);
	    }
	    if (motoActualizada.getId_usuario() != null) {
	        motoDB.setId_usuario(motoActualizada.getId_usuario());
	    }

	    Moto guardada = motoRepository.save(motoDB);
	    // Disparar verificación de alertas async cuando el km aumenta
	    if (kmActualizado) {
	        alertaService.verificarYEnviarAlertas(guardada);
	    }
	    return guardada;
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