package com.example.tactech_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Empresa cliente de TacTech. Sus usuarios con rol CLIENTE crean solicitudes. */
@Entity
@Table(name = "empresas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    /** RUT normalizado sin puntos: 76123456-0 */
    @Column(nullable = false, unique = true, length = 12)
    private String rut;

    @Column(nullable = false, length = 120)
    private String direccion;

    @Column(length = 15)
    private String telefono;
}
