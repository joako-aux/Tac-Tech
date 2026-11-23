package com.example.tactech_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Equipo de climatizacion atendido. El id (UUID) lo genera la app al crearlo offline. */
@Entity
@Table(name = "equipos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipo {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String modelo;

    @Column(nullable = false, length = 20)
    private String serie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Propiedad propiedad;
}
