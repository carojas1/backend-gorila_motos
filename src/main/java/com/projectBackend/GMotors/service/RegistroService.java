package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.dto.RegistroCreateDTO;
import com.projectBackend.GMotors.dto.RegistroDetalleDTO;
import com.projectBackend.GMotors.dto.RegistroListadoDTO;
import com.projectBackend.GMotors.dto.DetalleFacturaDTO;
import com.projectBackend.GMotors.dto.DetalleFacturaCreateDTO;
import com.projectBackend.GMotors.model.Factura;
import com.projectBackend.GMotors.model.Moto;
import com.projectBackend.GMotors.model.Registro;
import com.projectBackend.GMotors.model.Tipo;
import com.projectBackend.GMotors.model.Usuario;
import com.projectBackend.GMotors.repository.MotoRepository;
import com.projectBackend.GMotors.repository.RegistroRepository;
import com.projectBackend.GMotors.repository.TipoRepository;
import com.projectBackend.GMotors.repository.UsuarioRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroService {

	private final RegistroRepository registroRepository;
	private final UsuarioRepository usuarioRepository;
	private final MotoRepository motoRepository;
	private final TipoRepository tipoRepository;
	private final FacturaService facturaService;

	public RegistroService(RegistroRepository registroRepository, UsuarioRepository usuarioRepository,
			MotoRepository motoRepository, TipoRepository tipoRepository, FacturaService facturaService) {
		this.registroRepository = registroRepository;
		this.usuarioRepository = usuarioRepository;
		this.motoRepository = motoRepository;
		this.tipoRepository = tipoRepository;
		this.facturaService = facturaService;
	}

	@Transactional
	public Registro crearRegistro(RegistroCreateDTO dto) {

		// 1️⃣ Validaciones de alto nivel
		if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
			throw new IllegalArgumentException("[BE:REG-SVC]: El registro debe contener al menos un detalle");
		}

		// 2️⃣ Obtener entidades base
		Usuario cliente = usuarioRepository.findById(dto.getIdCliente())
				.orElseThrow(() -> new RuntimeException("[BE:REG-SVC]:Cliente no encontrado"));

		Usuario encargado = usuarioRepository.findById(dto.getIdEncargado())
				.orElseThrow(() -> new RuntimeException("[BE:REG-SVC]:Encargado no encontrado"));

		Moto moto = motoRepository.findById(dto.getIdMoto())
				.orElseThrow(() -> new RuntimeException("[BE:REG-SVC]:Moto no encontrada"));

		Tipo tipo = tipoRepository.findById(dto.getIdTipo())
				.orElseThrow(() -> new RuntimeException("[BE:REG-SVC]:Tipo de mantenimiento no encontrado"));

		// Agregar automáticamente el producto del tipo a los detalles
		List<DetalleFacturaCreateDTO> detallesConTipo = new ArrayList<>(dto.getDetalles());

		
		// 4️⃣ Crear factura (delegado) con detalles incluyendo el tipo
		Factura factura = facturaService.crearFactura(detallesConTipo, cliente.getId_usuario());

		// 5️⃣ Crear registro
		Registro registro = new Registro();
		registro.setFecha(java.time.LocalDate.now());
		registro.setObservaciones(dto.getObservaciones());
		registro.setEstado(dto.getEstado());
		registro.setKilometraje(dto.getKilometraje());

		registro.setFactura(factura);
		registro.setCliente(cliente);
		registro.setEncargado(encargado);
		registro.setMoto(moto);
		registro.setTipo(tipo);

		// 6️⃣ Persistir registro
		return registroRepository.save(registro);
	}

	/**
	 * Actualiza únicamente el estado de un registro
	 * 
	 * @param idRegistro  ID del registro a actualizar
	 * @param nuevoEstado Nuevo estado del registro
	 * @return Registro actualizado
	 */
	@Transactional
	public Registro actualizarEstado(Long idRegistro, Integer nuevoEstado, String observaciones) {

		// Validar que el estado no sea nulo
		if (nuevoEstado == null) {
			throw new IllegalArgumentException("[BE:REG-SVC]: El estado no puede ser nulo");
		}

		// Buscar el registro
		Registro registro = registroRepository.findById(idRegistro)
				.orElseThrow(() -> new RuntimeException("[BE:REG-SVC]: Registro no encontrado con ID: " + idRegistro));

		// Actualizar solo el estado
		registro.setEstado(nuevoEstado);
        
		 // Actualizar observaciones si vienen
	    if (observaciones != null) {
	        registro.setObservaciones(observaciones);
	    }
	    
		// Guardar y retornar
		return registroRepository.save(registro);
	}

	// ================= LISTAR TODOS =================
	@Transactional(readOnly = true)
	public List<RegistroListadoDTO> listarTodos() {
		return registroRepository.findAll().stream().map(this::mapToDTO).toList();
	}

	// ================= LISTAR POR CLIENTE =================
	@Transactional(readOnly = true)
	public List<RegistroListadoDTO> listarPorCliente(Long idCliente) {
		return registroRepository.findByCliente_IdUsuario(idCliente).stream().map(this::mapToDTO).toList();
	}

	// ================= HISTORIAL DE MANTENIMIENTOS POR CLIENTE =================
	@Transactional(readOnly = true)
	public List<RegistroListadoDTO> obtenerHistorialPorCliente(Long idCliente) {
		// Validar que el cliente existe
		usuarioRepository.findById(idCliente)
				.orElseThrow(() -> new RuntimeException("[BE:REG-SVC]: Cliente no encontrado"));

		return registroRepository.findByCliente_IdUsuarioOrderByFechaDesc(idCliente).stream().map(this::mapToDTO)
				.toList();
	}

	// ================= BUSCAR POR NOMBRE DE CLIENTE =================
	@Transactional(readOnly = true)
	public List<RegistroListadoDTO> buscarPorNombreCliente(String nombreCliente) {
		if (nombreCliente == null || nombreCliente.isBlank()) {
			throw new IllegalArgumentException("[BE:REG-SVC]: El nombre del cliente no puede estar vacío");
		}

		return registroRepository.buscarPorNombreCliente(nombreCliente).stream().map(this::mapToDTO).toList();
	}

	// ================= BUSCAR POR PLACA DE MOTO =================
	@Transactional(readOnly = true)
	public List<RegistroListadoDTO> buscarHistorialPorPlaca(String placa) {
	    if (placa == null || placa.isBlank()) {
	        throw new IllegalArgumentException("[BE:REG-SVC]: La placa no puede estar vacía");
	    }
	    return registroRepository.findByMoto_PlacaContainingIgnoreCase(placa)
	            .stream()
	            .map(this::mapToDTO)  
	            .collect(Collectors.toList());
	}

	// ================= LISTAR POR ENCARGADO =================
	@Transactional(readOnly = true)
	public List<RegistroListadoDTO> listarPorEncargado(Long idEncargado) {
		return registroRepository.findByEncargado_IdUsuario(idEncargado).stream().map(this::mapToDTO).toList();
	}

	// ================= MAPEO A DTO =================
	private RegistroListadoDTO mapToDTO(Registro registro) {

		RegistroListadoDTO dto = new RegistroListadoDTO();

		dto.setIdRegistro(registro.getIdRegistro());
		dto.setFecha(registro.getFecha());
		dto.setDescripcion(registro.getObservaciones());
		dto.setEstado(registro.getEstado());

		dto.setNombreCliente(registro.getCliente().getNombre_completo());

		dto.setMarcaMoto(registro.getMoto().getMarca());

		dto.setModeloMoto(registro.getMoto().getModelo());

		dto.setRutaImagenMoto(registro.getMoto().getRutaImagenMotos());

		dto.setPlacaMoto(registro.getMoto().getPlaca());

		dto.setTipoMantenimiento(registro.getTipo().getNombre());
		dto.setKilometraje(registro.getKilometraje());

		if (registro.getFactura() != null) {
			// MAPEAR ID DE FACTURA
			dto.setIdFactura(registro.getFactura().getIdFactura());

			if (registro.getFactura().getCostoTotal() != null) {
				dto.setCostoTotal(registro.getFactura().getCostoTotal().doubleValue());
			}
		}

		return dto;
	}

	public RegistroListadoDTO obtenerDetalle(Long idRegistro) {

		Registro registro = registroRepository.findById(idRegistro)
				.orElseThrow(() -> new RuntimeException("Registro no encontrado"));

		return mapToDTO(registro);
	}

}