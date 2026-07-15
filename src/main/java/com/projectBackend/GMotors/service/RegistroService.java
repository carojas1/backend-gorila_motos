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
import com.projectBackend.GMotors.repository.DetalleFacturaRepository;
import com.projectBackend.GMotors.model.DetalleFactura;

import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroService {

	private final RegistroRepository registroRepository;
	private final UsuarioRepository usuarioRepository;
	private final MotoRepository motoRepository;
	private final TipoRepository tipoRepository;
	private final FacturaService facturaService;
	private final DetalleFacturaRepository detalleFacturaRepository;

	public RegistroService(RegistroRepository registroRepository, UsuarioRepository usuarioRepository,
			MotoRepository motoRepository, TipoRepository tipoRepository, FacturaService facturaService,
			DetalleFacturaRepository detalleFacturaRepository) {
		this.registroRepository = registroRepository;
		this.usuarioRepository = usuarioRepository;
		this.motoRepository = motoRepository;
		this.tipoRepository = tipoRepository;
		this.facturaService = facturaService;
		this.detalleFacturaRepository = detalleFacturaRepository;
	}

	@Transactional
	public Registro crearRegistro(RegistroCreateDTO dto) {

		// 1️⃣ Validaciones de alto nivel
		if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
			throw new IllegalArgumentException("[BE:REG-SVC]: El registro debe contener al menos un detalle");
		}
		if (dto.getKilometraje() != null && dto.getKilometraje() < 0) {
			throw new IllegalArgumentException("El kilometraje no puede ser negativo");
		}
		if (dto.getEstado() == null || dto.getEstado() < 0 || dto.getEstado() > 4) {
			throw new IllegalArgumentException("El estado del registro debe estar entre 0 y 4");
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
		factura.setClienteNombre(cliente.getNombre_completo());
		factura.setClienteCedula(cliente.getCedula());
		factura.setClienteTelefono(cliente.getTelefono());
		factura.setClienteCorreo(cliente.getCorreo());
		factura.setClienteDireccion(cliente.getDireccion());
		factura.setClienteTipo("CLIENTE_TALLER");
		factura.setOrigenVenta("TALLER");
		factura = facturaService.save(factura);

		// 5️⃣ Crear registro
		Registro registro = new Registro();
		LocalDate fechaServicio = dto.getFecha() != null ? dto.getFecha() : LocalDate.now();
		if (dto.getFechaEntregaEstimada() != null && dto.getFechaEntregaEstimada().isBefore(fechaServicio)) {
			throw new IllegalArgumentException("La entrega estimada no puede ser anterior a la fecha del servicio");
		}
		registro.setFecha(fechaServicio);
		registro.setFechaEntregaEstimada(dto.getFechaEntregaEstimada());
		registro.setObservaciones(dto.getObservaciones());
		registro.setEstado(dto.getEstado());
		registro.setKilometraje(dto.getKilometraje());

		registro.setFactura(factura);
		registro.setCliente(cliente);
		registro.setEncargado(encargado);
		registro.setMoto(moto);
		registro.setTipo(tipo);
		aplicarFechasEstado(registro, dto.getEstado(), fechaServicio);

		if (dto.getKilometraje() != null
				&& (moto.getKilometraje() == null || dto.getKilometraje() > moto.getKilometraje())) {
			moto.setKilometraje(dto.getKilometraje());
			motoRepository.save(moto);
		}

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
	public Registro actualizarEstado(Long idRegistro, Integer nuevoEstado, String observaciones,
			LocalDate fechaEntregaEstimada) {

		// Validar que el estado no sea nulo
		if (nuevoEstado == null) {
			throw new IllegalArgumentException("[BE:REG-SVC]: El estado no puede ser nulo");
		}
		if (nuevoEstado < 0 || nuevoEstado > 4) {
			throw new IllegalArgumentException("El estado del registro debe estar entre 0 y 4");
		}

		// Buscar el registro
		Registro registro = registroRepository.findById(idRegistro)
				.orElseThrow(() -> new RuntimeException("[BE:REG-SVC]: Registro no encontrado con ID: " + idRegistro));

		if (fechaEntregaEstimada != null) {
			if (registro.getFecha() != null && fechaEntregaEstimada.isBefore(registro.getFecha())) {
				throw new IllegalArgumentException("La entrega estimada no puede ser anterior a la fecha del servicio");
			}
			registro.setFechaEntregaEstimada(fechaEntregaEstimada);
		}

		registro.setEstado(nuevoEstado);
		aplicarFechasEstado(registro, nuevoEstado, LocalDate.now());
        
		 // Actualizar observaciones si vienen
	    if (observaciones != null) {
	        registro.setObservaciones(observaciones);
	    }
	    
		// Guardar y retornar
		return registroRepository.save(registro);
	}

	private void aplicarFechasEstado(Registro registro, Integer estado, LocalDate fechaCambio) {
		if (estado == null) return;
		if (estado >= 2 && registro.getFechaCompletado() == null) {
			registro.setFechaCompletado(fechaCambio);
		}
		if (estado >= 3 && registro.getFechaEntregado() == null) {
			registro.setFechaEntregado(fechaCambio);
		}
		if (estado >= 4 && registro.getFechaFacturado() == null) {
			registro.setFechaFacturado(fechaCambio);
		}
	}

	// ================= LISTAR TODOS =================
	@Transactional(readOnly = true)
	public List<RegistroListadoDTO> listarTodos() {
		return mapToDTOs(registroRepository.findAll(), true);
	}

	@Transactional(readOnly = true)
	public List<RegistroListadoDTO> listarResumen() {
		return mapToDTOs(registroRepository.findAll(), false);
	}

	// ================= LISTAR POR CLIENTE =================
	@Transactional(readOnly = true)
	public List<RegistroListadoDTO> listarPorCliente(Long idCliente) {
		return mapToDTOs(registroRepository.findByCliente_IdUsuario(idCliente), true);
	}

	// ================= HISTORIAL DE MANTENIMIENTOS POR CLIENTE =================
	@Transactional(readOnly = true)
	public List<RegistroListadoDTO> obtenerHistorialPorCliente(Long idCliente) {
		// Validar que el cliente existe
		usuarioRepository.findById(idCliente)
				.orElseThrow(() -> new RuntimeException("[BE:REG-SVC]: Cliente no encontrado"));

		return mapToDTOs(registroRepository.findByCliente_IdUsuarioOrderByFechaDesc(idCliente), true);
	}

	// ================= BUSCAR POR NOMBRE DE CLIENTE =================
	@Transactional(readOnly = true)
	public List<RegistroListadoDTO> buscarPorNombreCliente(String nombreCliente) {
		if (nombreCliente == null || nombreCliente.isBlank()) {
			throw new IllegalArgumentException("[BE:REG-SVC]: El nombre del cliente no puede estar vacío");
		}

		return mapToDTOs(registroRepository.buscarPorNombreCliente(nombreCliente), true);
	}

	// ================= BUSCAR POR PLACA DE MOTO =================
	@Transactional(readOnly = true)
	public List<RegistroListadoDTO> buscarHistorialPorPlaca(String placa) {
	    if (placa == null || placa.isBlank()) {
	        throw new IllegalArgumentException("[BE:REG-SVC]: La placa no puede estar vacía");
	    }
	    return mapToDTOs(registroRepository.findByMoto_PlacaContainingIgnoreCase(placa), true);
	}

	// ================= LISTAR POR ENCARGADO =================
	@Transactional(readOnly = true)
	public List<RegistroListadoDTO> listarPorEncargado(Long idEncargado) {
		return mapToDTOs(registroRepository.findByEncargado_IdUsuario(idEncargado), true);
	}

	// ================= MAPEO A DTO =================
	private List<RegistroListadoDTO> mapToDTOs(List<Registro> registros, boolean incluirDetalles) {
		if (registros.isEmpty()) return Collections.emptyList();

		Map<Long, List<DetalleFactura>> detallesPorFactura = new HashMap<>();
		if (incluirDetalles) {
			List<Long> idsFactura = registros.stream()
					.filter(r -> r.getFactura() != null)
					.map(r -> r.getFactura().getIdFactura())
					.distinct()
					.toList();
			for (DetalleFactura detalle : detalleFacturaRepository.findByIdFacturaIn(idsFactura)) {
				detallesPorFactura.computeIfAbsent(detalle.getIdFactura(), ignored -> new ArrayList<>()).add(detalle);
			}
		}

		return registros.stream()
				.map(registro -> mapToDTO(registro, detallesPorFactura, incluirDetalles))
				.toList();
	}

	private RegistroListadoDTO mapToDTO(Registro registro, Map<Long, List<DetalleFactura>> detallesPorFactura,
			boolean incluirDetalles) {

		RegistroListadoDTO dto = new RegistroListadoDTO();

		dto.setIdRegistro(registro.getIdRegistro());
		dto.setIdMoto(registro.getMoto().getIdMoto());
		dto.setFecha(registro.getFecha());
		dto.setFechaEntregaEstimada(registro.getFechaEntregaEstimada());
		dto.setFechaCompletado(registro.getFechaCompletado());
		dto.setFechaEntregado(registro.getFechaEntregado());
		dto.setFechaFacturado(registro.getFechaFacturado());
		dto.setDescripcion(registro.getObservaciones());
		dto.setObservaciones(registro.getObservaciones());
		dto.setEstado(registro.getEstado());

		dto.setIdCliente(registro.getCliente().getId_usuario());
		dto.setNombreCliente(registro.getCliente().getNombre_completo());
		dto.setCedulaCliente(registro.getCliente().getCedula());
		dto.setTelefonoCliente(registro.getCliente().getTelefono());
		dto.setCorreoCliente(registro.getCliente().getCorreo());
		dto.setDireccionCliente(registro.getCliente().getDireccion());

		if (registro.getEncargado() != null) {
			dto.setIdEncargado(registro.getEncargado().getId_usuario());
			dto.setNombreEncargado(registro.getEncargado().getNombre_completo());
		}

		dto.setMarcaMoto(registro.getMoto().getMarca());

		dto.setModeloMoto(registro.getMoto().getModelo());

		dto.setRutaImagenMoto(registro.getMoto().getRutaImagenMotos());

		dto.setPlacaMoto(registro.getMoto().getPlaca());

		dto.setTipoMantenimiento(registro.getTipo().getNombre());
		dto.setKilometraje(registro.getKilometraje());

		if (registro.getFactura() != null) {
			Long idFactura = registro.getFactura().getIdFactura();
			dto.setIdFactura(idFactura);
			List<DetalleFacturaDTO> detalles = incluirDetalles
					? DetalleFacturaDTO.mapToDTOList(detallesPorFactura.getOrDefault(idFactura, Collections.emptyList()))
					: Collections.emptyList();
			if (incluirDetalles) dto.setDetalles(detalles);
			BigDecimal totalReal = detalles.stream()
					.map(d -> d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			if (!detalles.isEmpty()) {
				dto.setCostoTotal(totalReal.setScale(2, RoundingMode.HALF_UP).doubleValue());
			} else if (registro.getFactura().getCostoTotal() != null) {
				dto.setCostoTotal(registro.getFactura().getCostoTotal().doubleValue());
			}
		}

		return dto;
	}

	public RegistroListadoDTO obtenerDetalle(Long idRegistro) {

		Registro registro = registroRepository.findById(idRegistro)
				.orElseThrow(() -> new RuntimeException("Registro no encontrado"));

		Map<Long, List<DetalleFactura>> detallesPorFactura = new HashMap<>();
		if (registro.getFactura() != null) {
			detallesPorFactura.put(registro.getFactura().getIdFactura(),
					detalleFacturaRepository.findByIdFactura(registro.getFactura().getIdFactura()));
		}
		return mapToDTO(registro, detallesPorFactura, true);
	}

}
