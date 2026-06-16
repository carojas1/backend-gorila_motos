package com.projectBackend.GMotors.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "diagnostico_moto")
public class DiagnosticoMoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_diagnostico")
    private Long idDiagnostico;

    @Column(name = "id_moto", nullable = false)
    private Long idMoto;

    @Column(name = "id_mecanico", nullable = false)
    private Long idMecanico;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "kilometraje_ingreso", nullable = false)
    private Integer kilometrajeIngreso;

    @Column(name = "observaciones_generales", columnDefinition = "TEXT")
    private String observacionesGenerales;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_diagnostico")
    private List<DetalleDiagnostico> detalles;

    public DiagnosticoMoto() {}

    @JsonProperty("id_diagnostico")
    public Long getIdDiagnostico()                                 { return idDiagnostico; }

    @JsonProperty("id_moto")
    public Long getIdMoto()                                        { return idMoto; }
    public void setIdMoto(Long idMoto)                             { this.idMoto = idMoto; }

    @JsonProperty("id_mecanico")
    public Long getIdMecanico()                                    { return idMecanico; }
    public void setIdMecanico(Long idMecanico)                     { this.idMecanico = idMecanico; }

    public LocalDateTime getFecha()                                { return fecha; }
    public void setFecha(LocalDateTime fecha)                      { this.fecha = fecha; }

    @JsonProperty("kilometraje_ingreso")
    public Integer getKilometrajeIngreso()                         { return kilometrajeIngreso; }
    public void setKilometrajeIngreso(Integer km)                  { this.kilometrajeIngreso = km; }

    @JsonProperty("observaciones_generales")
    public String getObservacionesGenerales()                      { return observacionesGenerales; }
    public void setObservacionesGenerales(String obs)              { this.observacionesGenerales = obs; }

    public List<DetalleDiagnostico> getDetalles()                  { return detalles; }
    public void setDetalles(List<DetalleDiagnostico> detalles)     { this.detalles = detalles; }
}
