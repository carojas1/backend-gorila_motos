package com.projectBackend.GMotors.controller;

import com.projectBackend.GMotors.dto.RegistroCreateDTO;
import com.projectBackend.GMotors.dto.RegistroDetalleDTO;
import com.projectBackend.GMotors.dto.RegistroListadoDTO;
import com.projectBackend.GMotors.dto.DetalleFacturaDTO;
import com.projectBackend.GMotors.dto.DetalleFacturaCreateDTO;
import com.projectBackend.GMotors.model.Registro;
import com.projectBackend.GMotors.model.Usuario;
import com.projectBackend.GMotors.model.Moto;
import com.projectBackend.GMotors.model.Tipo;
import com.projectBackend.GMotors.repository.RegistroRepository;
import com.projectBackend.GMotors.repository.UsuarioRepository;
import com.projectBackend.GMotors.repository.MotoRepository;
import com.projectBackend.GMotors.model.Factura;
import com.projectBackend.GMotors.service.RegistroService;
import com.projectBackend.GMotors.service.FacturaService;
import com.projectBackend.GMotors.service.ResendEmailService;
import com.projectBackend.GMotors.config.FlaskOcrClient;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/registros")
public class RegistroController {

	private final RegistroService registroService;

	@Autowired
	private FlaskOcrClient flaskOcrClient;

	@Autowired
	private FacturaService facturaService;

	@Autowired
	private ResendEmailService emailService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private MotoRepository motoRepository;

	@Autowired
	private RegistroRepository registroRepository;
	

	public RegistroController(RegistroService registroService) {
		this.registroService = registroService;
	}

	// ✅ Crear registro (factura + detalles incluidos)
	@PostMapping
	public ResponseEntity<?> crearRegistro(@RequestBody RegistroCreateDTO dto) {
		try {
			Registro registro = registroService.crearRegistro(dto);
			return ResponseEntity.status(HttpStatus.CREATED).body(registro);

		} catch (IllegalArgumentException e) {
			// Errores de validación de negocio
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

		} catch (RuntimeException e) {
			// Entidades no encontradas u otros errores controlados
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

		} catch (Exception e) {
			// Error inesperado
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear el registro");
		}
	}

	@PostMapping("/test")
	public ResponseEntity<?> test(@RequestBody Map<String, Object> body) {
		System.out.println("BODY = " + body);
		return ResponseEntity.ok(body);
	}

	// =====================================================
	// ACTUALIZAR FACTURA
	// =====================================================
	@PutMapping("/{idRegistro}/factura")
	public ResponseEntity<Map<String, Object>> actualizarFactura(@PathVariable Long idRegistro,
			@RequestBody List<DetalleFacturaCreateDTO> detallesDTO) {
		System.out.println("[RegistroController] PUT /api/registros/" + idRegistro + "/factura");
		System.out.println(" Detalles recibidos: " + detallesDTO);
		Long idFactura;

		Registro registro = registroRepository.findById(idRegistro)
				.orElseThrow(() -> new RuntimeException("[BE:REG-SVC]: Registro no encontrado con ID: " + idRegistro));		
		// Validar que el estado no sea nulo
		
		
		Factura facturaEntity = registro.getFactura();
		Long idfactura = facturaEntity.getIdFactura();
		

		try {
			Factura factura = facturaService.actualizarFactura(idfactura, detallesDTO);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("mensaje", "Factura actualizada exitosamente");
			response.put("idFactura", factura.getIdFactura());
			response.put("costoTotal", factura.getCostoTotal());
			response.put("fechaEmision", factura.getFechaEmision());

			return ResponseEntity.ok(response);

		} catch (IllegalArgumentException e) {
			Map<String, Object> error = new HashMap<>();
			error.put("success", false);
			error.put("error", e.getMessage());
			return ResponseEntity.badRequest().body(error);

		} catch (Exception e) {
			Map<String, Object> error = new HashMap<>();
			error.put("success", false);
			error.put("error", "Error al actualizar factura: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
		}
	}

	/**
	 * Actualizar solo el estado de un registro — acepta JSON body: { "estado": 2, "observaciones": "..." }
	 */
	@PatchMapping("/{id}/estado")
	public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @RequestBody Map<String, Object> body) {
		Integer estado = body.containsKey("estado") ? ((Number) body.get("estado")).intValue() : null;
		String observaciones = body.containsKey("observaciones") ? (String) body.get("observaciones") : null;

		try {
			Registro registroActualizado = registroService.actualizarEstado(id, estado, observaciones);

			// ── Email automático cuando se factura (estado = 4) ──────────────────
			if (estado != null && estado == 4) {
				try {
					Registro reg = registroRepository.findById(id).orElse(null);
					if (reg != null && reg.getCliente() != null && reg.getCliente().getCorreo() != null) {
						Usuario cliente = reg.getCliente();
						Moto    moto    = reg.getMoto();
						double  costo   = reg.getFactura() != null ? reg.getFactura().getCostoTotal() : 0.0;
						String  tipo    = reg.getTipo() != null ? reg.getTipo().getNombre() : "Servicio de mantenimiento";
						String  fecha   = reg.getFecha() != null ? reg.getFecha().toString() : "—";
						String  placa   = moto != null ? moto.getPlaca() : "—";

						emailService.enviarFactura(
							cliente.getCorreo(),
							cliente.getNombreCompleto(),
							placa, tipo, costo, fecha, id
						);
					}
				} catch (Exception emailEx) {
					System.err.println("[EMAIL] Error enviando factura: " + emailEx.getMessage());
					// No bloqueamos la respuesta si el email falla
				}
			}

			return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado exitosamente", "idRegistro",
					registroActualizado.getIdRegistro(), "nuevoEstado", registroActualizado.getEstado()));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));

		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Error al actualizar el estado: " + e.getMessage()));
		}
	}

	// =====================================================
	// OBTENER FACTURAS POR USUARIO
	// =====================================================
	@GetMapping("/usuario/{idUsuario}")
	public ResponseEntity<Map<String, Object>> obtenerFacturasPorUsuario(@PathVariable Long idUsuario) {
		try {
			var facturas = facturaService.obtenerFacturasPorUsuario(idUsuario);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("facturas", facturas);
			response.put("total", facturas.size());

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			Map<String, Object> error = new HashMap<>();
			error.put("success", false);
			error.put("error", "Error al obtener facturas: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
		}
	}

	// =====================================================
	// ELIMINAR FACTURA
	// =====================================================
	@DeleteMapping("/{idFactura}")
	public ResponseEntity<Map<String, Object>> eliminarFactura(@PathVariable Long idFactura) {
		try {
			facturaService.eliminarFactura(idFactura);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("mensaje", "Factura eliminada exitosamente");

			return ResponseEntity.ok(response);

		} catch (IllegalArgumentException e) {
			Map<String, Object> error = new HashMap<>();
			error.put("success", false);
			error.put("error", e.getMessage());
			return ResponseEntity.badRequest().body(error);

		} catch (Exception e) {
			Map<String, Object> error = new HashMap<>();
			error.put("success", false);
			error.put("error", "Error al eliminar factura: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
		}
	}

	// ControllerREG
	// ================= LISTAR TODOS =================
	@GetMapping
	public ResponseEntity<List<RegistroListadoDTO>> listarTodos() {
		return ResponseEntity.ok(registroService.listarTodos());
	}

	// ================= LISTAR POR CLIENTE =================
	@GetMapping("/cliente/{idCliente}")
	public ResponseEntity<List<RegistroListadoDTO>> listarPorCliente(@PathVariable Long idCliente) {
		return ResponseEntity.ok(registroService.listarPorCliente(idCliente));
	}

	// ================= LISTAR POR ENCARGADO =================
	@GetMapping("/encargado/{idEncargado}")
	public ResponseEntity<List<RegistroListadoDTO>> listarPorEncargado(@PathVariable Long idEncargado) {
		return ResponseEntity.ok(registroService.listarPorEncargado(idEncargado));
	}

	// ================= HISTORIAL DE MANTENIMIENTOS POR USUARIO =================
	@GetMapping("/historial/{idCliente}")
	public ResponseEntity<?> obtenerHistorialMantenimientos(@PathVariable Long idCliente) {
		try {
			List<RegistroListadoDTO> historial = registroService.obtenerHistorialPorCliente(idCliente);
			return ResponseEntity.ok(historial);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error al obtener historial: " + e.getMessage());
		}
	}
	
	// // ================= Historial de Usuarios =================
	@GetMapping("/buscar/placa")
	public ResponseEntity<List<RegistroListadoDTO>> buscarPorPlaca( 
	        @RequestParam String placa) {
	    try {
	        List<RegistroListadoDTO> historial = registroService.buscarHistorialPorPlaca(placa);
	        return ResponseEntity.ok(historial);
	    } catch (IllegalArgumentException e) {
	        return ResponseEntity.badRequest().build();
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	    }
	}


	@GetMapping("/{idFactura}/detalles-factura")
	public ResponseEntity<List<DetalleFacturaDTO>> obtenerDetallesFactura(@PathVariable Long idFactura) {
		try {
			List<DetalleFacturaDTO> detalles = facturaService.obtenerDetallesPorFactura(idFactura);
			return ResponseEntity.ok(detalles);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	// ================= BUSCAR HISTORIAL POR PLACA (CON OCR) =================
	@PostMapping("/ocr/historial")
	public ResponseEntity<?> obtenerHistorialPorPlacaOCR(@RequestParam("image") MultipartFile image) {
		try {
			System.out.println("[REGISTRO-CONTROLLER] Recibiendo imagen para OCR...");

			// Detectar placa con OCR (usando FlaskOcrClient)
			String placaDetectada = flaskOcrClient.detectarPlaca(image);

			if (placaDetectada == null || placaDetectada.isBlank()) {
				System.out.println("❌ No se detectó placa");
				return ResponseEntity
						.ok(Map.of("success", false, "mensaje", "No se pudo detectar la placa en la imagen"));
			}

			// System.out.println("Placa detectada: " + placaDetectada);

			// 2Buscar historial por placa
			List<RegistroListadoDTO> historial = registroService.buscarHistorialPorPlaca(placaDetectada);

			if (historial.isEmpty()) {
				System.out.println("No hay registros para la placa: " + placaDetectada);
				return ResponseEntity.ok(Map.of("success", false, "placa", placaDetectada, "mensaje",
						"No hay historial de mantenimientos para esta placa"));
			}

			// System.out.println("Encontrados " + historial.size() + " registros");
			return ResponseEntity.ok(Map.of("success", true, "placa", placaDetectada, "historial", historial));

		} catch (Exception e) {
			System.err.println("❌ Error en OCR historial:");
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "mensaje", "Error al procesar la solicitud: " + e.getMessage()));
		}
	}

	// ================= BUSCAR POR NOMBRE DE CLIENTE =================
	@GetMapping("/buscar/nombre")
	public ResponseEntity<?> buscarPorNombre(@RequestParam String nombreCliente) {
		try {
			List<RegistroListadoDTO> resultados = registroService.buscarPorNombreCliente(nombreCliente);
			return ResponseEntity.ok(resultados);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error al buscar registros: " + e.getMessage());
		}
	}

	@GetMapping("/{id}")
	public RegistroListadoDTO obtenerDetalle(@PathVariable Long id) {
		return registroService.obtenerDetalle(id);
	}
}
