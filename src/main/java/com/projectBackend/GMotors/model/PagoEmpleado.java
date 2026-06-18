package com.projectBackend.GMotors.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pagos_empleado")
public class PagoEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long idPago;

    /** 0 = gasto general (compra inventario, etc.) */
    @Column(name = "id_empleado", nullable = false)
    private Long idEmpleado;

    @Column(nullable = false)
    private LocalDate fecha;

    /** Sueldo | Bono | Comisión | Anticipo | Compra inventario | Otro */
    @Column(nullable = false, length = 50)
    private String concepto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(length = 300)
    private String notas;

    @JsonProperty("id_pago")
    public Long getIdPago() { return idPago; }
    public void setIdPago(Long idPago) { this.idPago = idPago; }

    @JsonProperty("id_empleado")
    public Long getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Long idEmpleado) { this.idEmpleado = idEmpleado; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
