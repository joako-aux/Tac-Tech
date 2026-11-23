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
 * Solicitud de servicio creada por un cliente para un equipo de frio.
 * Nace en POR_CONFIRMAR; el administrador la convierte en ticket o la rechaza.
 */
@Entity
@Table(name = "solicitudes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Solicitud {

    /** UUID generado en el telefono: el cliente puede crearla sin conexion. */
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    @ToString.Exclude
    private Usuario cliente;

    @Column(nullable = false, length = 120)
    private String clienteCorreo;

    @Column(nullable = false, length = 60)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, length = 120)
    private String direccion;

    @Column(nullable = false, length = 60)
    private String equipoNombre;

    @Column(nullable = false, length = 40)
    private String equipoModelo;

    @Column(nullable = false, length = 20)
    private String equipoSerie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Propiedad propiedad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado;

    /** Motivo de cancelacion (cliente) o de rechazo (administrador). */
    @Column(length = 300)
    private String motivo;

    /** Ticket creado al confirmar. */
    @Column(length = 36)
    private String ticketId;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;
}
