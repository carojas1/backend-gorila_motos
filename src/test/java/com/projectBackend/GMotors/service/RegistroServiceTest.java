package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.model.Registro;
import com.projectBackend.GMotors.repository.DetalleFacturaRepository;
import com.projectBackend.GMotors.repository.MotoRepository;
import com.projectBackend.GMotors.repository.RegistroRepository;
import com.projectBackend.GMotors.repository.TipoRepository;
import com.projectBackend.GMotors.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroServiceTest {

    @Mock private RegistroRepository registroRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MotoRepository motoRepository;
    @Mock private TipoRepository tipoRepository;
    @Mock private FacturaService facturaService;
    @Mock private DetalleFacturaRepository detalleFacturaRepository;

    private RegistroService registroService;

    @BeforeEach
    void setUp() {
        registroService = new RegistroService(registroRepository, usuarioRepository, motoRepository,
                tipoRepository, facturaService, detalleFacturaRepository);
    }

    @Test
    void completarConservaFechaServicioYRegistraFechaDelCambio() {
        Registro registro = new Registro();
        registro.setFecha(LocalDate.of(2026, 7, 12));
        registro.setEstado(1);
        registro.setObservaciones("Revision de frenos");
        when(registroRepository.findById(10L)).thenReturn(Optional.of(registro));
        when(registroRepository.save(any(Registro.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Registro actualizado = registroService.actualizarEstado(10L, 2, null,
                LocalDate.of(2026, 7, 14));

        assertEquals(LocalDate.of(2026, 7, 12), actualizado.getFecha());
        assertEquals(LocalDate.of(2026, 7, 14), actualizado.getFechaEntregaEstimada());
        assertEquals(LocalDate.now(), actualizado.getFechaCompletado());
    }

    @Test
    void rechazaEntregaAnteriorAlIngreso() {
        Registro registro = new Registro();
        registro.setFecha(LocalDate.of(2026, 7, 12));
        registro.setEstado(1);
        when(registroRepository.findById(11L)).thenReturn(Optional.of(registro));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> registroService.actualizarEstado(11L, 2, null, LocalDate.of(2026, 7, 11)));

        assertTrue(error.getMessage().contains("anterior"));
    }
}
