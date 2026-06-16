package com.projectBackend.GMotors.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerta_enviada")
public class AlertaEnviada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Long idAlerta;

    @Column(name = "id_moto", nullable = false)
    private Long idMoto;

    @Column(nullable = false, length = 80)
    private String tipo;

    @Column(name = "km_umbral", nullable = false)
    private Integer kmUmbral;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    public AlertaEnviada() {}

    public AlertaEnviada(Long idMoto, String tipo, Integer kmUmbral, LocalDateTime fechaEnvio) {
        this.idMoto     = idMoto;
        this.tipo       = tipo;
        this.kmUmbral   = kmUmbral;
        this.fechaEnvio = fechaEnvio;
    }

    @JsonProperty("id_alerta")
    public Long getIdAlerta()                                   { return idAlerta; }

    @JsonProperty("id_moto")
    public Long getIdMoto()                                     { return idMoto; }
    public void setIdMoto(Long idMoto)                          { this.idMoto = idMoto; }

    public String getTipo()                                     { return tipo; }
    public void setTipo(String tipo)                            { this.tipo = tipo; }

    @JsonProperty("km_umbral")
    public Integer getKmUmbral()                                { return kmUmbral; }
    public void setKmUmbral(Integer kmUmbral)                   { this.kmUmbral = kmUmbral; }

    @JsonProperty("fecha_envio")
    public LocalDateTime getFechaEnvio()                        { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio)         { this.fechaEnvio = fechaEnvio; }
}
