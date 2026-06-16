package com.projectBackend.GMotors.service;

import com.projectBackend.GMotors.model.*;
import com.projectBackend.GMotors.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DiagnosticoService {

    @Autowired private DiagnosticoMotoRepository diagnosticoRepo;
    @Autowired private MotoRepository            motoRepo;

    @Transactional
    public DiagnosticoMoto crear(DiagnosticoMoto diagnostico) {
        if (diagnostico.getFecha() == null) {
            diagnostico.setFecha(LocalDateTime.now());
        }
        // Actualizar km de la moto con el km de ingreso
        if (diagnostico.getKilometrajeIngreso() != null && diagnostico.getIdMoto() != null) {
            motoRepo.findById(diagnostico.getIdMoto()).ifPresent(moto -> {
                moto.setKilometraje(diagnostico.getKilometrajeIngreso());
                motoRepo.save(moto);
            });
        }
        return diagnosticoRepo.save(diagnostico);
    }

    public Optional<DiagnosticoMoto> buscarPorId(Long id) {
        return diagnosticoRepo.findById(id);
    }

    public List<DiagnosticoMoto> historialPorMoto(Long idMoto) {
        return diagnosticoRepo.findByIdMotoOrderByFechaDesc(idMoto);
    }

    public List<DiagnosticoMoto> porMecanico(Long idMecanico) {
        return diagnosticoRepo.findByIdMecanicoOrderByFechaDesc(idMecanico);
    }
}
