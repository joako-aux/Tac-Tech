package com.example.tactech_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Ticket de atencion. Se relaciona con el equipo atendido y con el tecnico asignado.
 * Se usa @Getter/@Setter en vez de @Data para que toString/equals no recorran las relaciones.
 */
@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, length = 60)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, length = 80)
    private String cliente;

    @Column(nullable = false, length = 120)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoTicket estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    @ToString.Exclude
    private Equipo equipo;

    /** Tecnico que creo el ticket. Puede ser null si el correo no existe en la tabla usuarios. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    @ToString.Exclude
    private Usuario tecnico;

    /** Correo tal como lo envio la app; se conserva aunque el usuario aun no exista en la tabla. */
    @Column(length = 120)
    private String tecnicoCorreo;

    /** Solicitud del cliente que dio origen a este ticket. */
    @Column(length = 36)
    private String solicitudId;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(length = 300)
    private String comentarioEstado;

    private LocalDateTime fechaCierre;

    @Column(length = 300)
    private String solucion;
}
