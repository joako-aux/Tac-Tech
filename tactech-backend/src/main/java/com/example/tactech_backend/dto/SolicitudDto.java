package com.example.tactech_backend.dto;

import com.example.tactech_backend.entity.EstadoSolicitud;
import com.example.tactech_backend.entity.Propiedad;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Mismas reglas que ValidadorSolicitud en la app. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudDto {

    @NotBlank(message = "El id es obligatorio")
    @Size(max = 36)
    private String id;

    @NotBlank(message = "El cliente es obligatorio")
    @Email(message = "El correo del cliente no es valido")
    private String clienteCorreo;

    /** Solo salida: los completa el servidor. */
    private String clienteNombre;
    private String empresaNombre;

    @NotBlank(message = "El titulo es obligatorio")
    @Size(min = 5, max = 60, message = "El titulo debe tener entre 5 y 60 caracteres")
    private String titulo;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(min = 10, max = 500, message = "La descripcion debe tener entre 10 y 500 caracteres")
    private String descripcion;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(min = 5, max = 120, message = "La direccion debe tener entre 5 y 120 caracteres")
    private String direccion;

    @NotBlank(message = "El nombre del equipo es obligatorio")
    @Size(min = 3, max = 60, message = "El nombre del equipo debe tener entre 3 y 60 caracteres")
    private String equipoNombre;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(min = 2, max = 40, message = "El modelo debe tener entre 2 y 40 caracteres")
    private String equipoModelo;

    @NotBlank(message = "La serie es obligatoria")
    @Pattern(regexp = "^[A-Z0-9-]{4,20}$", message = "La serie admite 4 a 20 letras mayusculas, numeros o guion")
    private String equipoSerie;

    @NotNull(message = "La propiedad del equipo es obligatoria")
    private Propiedad propiedad;

    @NotNull(message = "El estado es obligatorio")
    private EstadoSolicitud estado;

    @Size(max = 300)
    private String motivo;

    private String ticketId;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;
}
