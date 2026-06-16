package com.projectBackend.GMotors.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "detalle_diagnostico")
public class DetalleDiagnostico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long idDetalle;

    @Column(nullable = false, length = 60)
    private String parte;

    @Column(nullable = false)
    private Integer estado; // 1=BUENO 2=REGULAR 3=MALO

    @Column(columnDefinition = "TEXT")
    private String observacion;

    public DetalleDiagnostico() {}

    public DetalleDiagnostico(String parte, Integer estado, String observacion) {
        this.parte       = parte;
        this.estado      = estado;
        this.observacion = observacion;
    }

    @JsonProperty("id_detalle")
    public Long getIdDetalle()                                { return idDetalle; }

    public String getParte()                                  { return parte; }
    public void setParte(String parte)                        { this.parte = parte; }

    public Integer getEstado()                                { return estado; }
    public void setEstado(Integer estado)                     { this.estado = estado; }

    public String getObservacion()                            { return observacion; }
    public void setObservacion(String observacion)            { this.observacion = observacion; }
}
