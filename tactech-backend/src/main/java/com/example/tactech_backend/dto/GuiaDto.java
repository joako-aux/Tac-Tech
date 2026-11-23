package com.example.tactech_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Mismas reglas que ValidadorGuia en la app. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuiaDto {

    private static final String SOLO_LETRAS = "^[\\p{L} .'-]{3,80}$";

    @NotBlank(message = "El id es obligatorio")
    @Size(max = 36)
    private String id;

    @NotNull(message = "El numero es obligatorio")
    @Positive
    private Integer numero;

    @NotBlank(message = "El ticket es obligatorio")
    private String ticketId;

    @NotBlank(message = "El equipo es obligatorio")
    private String equipoId;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(min = 5, max = 120, message = "La direccion debe tener entre 5 y 120 caracteres")
    private String direccion;

    /** Evidencia obligatoria: la geolocalizacion de la visita. */
    @NotNull(message = "La ubicacion de la visita es obligatoria")
    private Double latitud;

    @NotNull(message = "La ubicacion de la visita es obligatoria")
    private Double longitud;

    private LocalDateTime fecha;

    @NotBlank(message = "El tecnico es obligatorio")
    @Pattern(regexp = SOLO_LETRAS, message = "El nombre del tecnico solo admite letras (3 a 80)")
    private String tecnico;

    @NotBlank(message = "El trabajo realizado es obligatorio")
    @Size(min = 10, max = 500, message = "El trabajo realizado debe tener entre 10 y 500 caracteres")
    private String trabajoRealizado;

    @Size(max = 200, message = "Repuestos admite hasta 200 caracteres")
    private String repuestos;

    @Size(max = 300, message = "Observaciones admite hasta 300 caracteres")
    private String observaciones;

    @NotBlank(message = "Quien recibe es obligatorio")
    @Pattern(regexp = SOLO_LETRAS, message = "Quien recibe solo admite letras (3 a 80)")
    private String recibidoPor;

    /** Evidencia obligatoria: al menos una fotografia. */
    @Valid
    @NotNull(message = "Debes adjuntar al menos una fotografia")
    @Size(min = 1, max = 15, message = "Debes adjuntar entre 1 y 15 fotografias")
    private List<FotoGuiaDto> fotos = new ArrayList<>();
}
