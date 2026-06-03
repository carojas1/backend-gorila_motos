package com.projectBackend.GMotors.dto;

public class QuickAccountDTO {
    private Long mecanicoId;
    private String nombre_completo;
    private String placa;

    // Constructor vacío
    public QuickAccountDTO() {
    }

    // Constructor completo
    public QuickAccountDTO(Long mecanicoId, String nombre_completo, String placa) {
        this.mecanicoId = mecanicoId;
        this.nombre_completo = nombre_completo;
        this.placa = placa;
    }

    // Getters y Setters
    public Long getMecanicoId() {
        return mecanicoId;
    }

    public void setMecanicoId(Long mecanicoId) {
        this.mecanicoId = mecanicoId;
    }

    public String getNombre_completo() {
        return nombre_completo;
    }

    public void setNombre_completo(String nombre_completo) {
        this.nombre_completo = nombre_completo;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }
}