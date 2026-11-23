package com.example.tactech_backend.dto;

import com.example.tactech_backend.entity.Propiedad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mismas reglas que ValidadorTicket en la app: el servidor no confia solo en el cliente. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipoDto {

    @NotBlank(message = "El id es obligatorio")
    @Size(max = 36)
    private String id;

    @NotBlank(message = "El nombre del equipo es obligatorio")
    @Size(min = 3, max = 60, message = "El nombre del equipo debe tener entre 3 y 60 caracteres")
    private String nombre;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(min = 2, max = 40, message = "El modelo debe tener entre 2 y 40 caracteres")
    private String modelo;

    @NotBlank(message = "La serie es obligatoria")
    @Pattern(regexp = "^[A-Z0-9-]{4,20}$", message = "La serie admite 4 a 20 letras mayusculas, numeros o guion")
    private String serie;

    @NotNull(message = "La propiedad es obligatoria")
    private Propiedad propiedad;
}
