package com.example.tactech_backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Guia de servicio emitida en una visita. Sus fotos se guardan y borran junto con ella. */
@Entity
@Table(name = "guias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Guia {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Integer numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    @ToString.Exclude
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    @ToString.Exclude
    private Equipo equipo;

    @Column(nullable = false, length = 120)
    private String direccion;

    private Double latitud;

    private Double longitud;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 80)
    private String tecnico;

    @Column(nullable = false, length = 500)
    private String trabajoRealizado;

    @Column(length = 200)
    private String repuestos;

    @Column(length = 300)
    private String observaciones;

    @Column(nullable = false, length = 80)
    private String recibidoPor;

    @OneToMany(mappedBy = "guia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaCaptura ASC")
    @Builder.Default
    @ToString.Exclude
    private List<FotoGuia> fotos = new ArrayList<>();
}
