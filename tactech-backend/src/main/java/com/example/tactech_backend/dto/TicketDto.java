package com.example.tactech_backend.dto;

import com.example.tactech_backend.entity.EstadoTicket;
import com.example.tactech_backend.entity.Prioridad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDto {

    @NotBlank(message = "El id es obligatorio")
    @Size(max = 36)
    private String id;

    @NotNull(message = "El numero es obligatorio")
    @Positive(message = "El numero debe ser positivo")
    private Integer numero;

    @NotBlank(message = "El titulo es obligatorio")
    @Size(min = 5, max = 60, message = "El titulo debe tener entre 5 y 60 caracteres")
    private String titulo;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(min = 10, max = 500, message = "La descripcion debe tener entre 10 y 500 caracteres")
    private String descripcion;

    @NotBlank(message = "El cliente es obligatorio")
    @Size(min = 3, max = 80, message = "El cliente debe tener entre 3 y 80 caracteres")
    private String cliente;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(min = 5, max = 120, message = "La direccion debe tener entre 5 y 120 caracteres")
    private String direccion;

    @NotNull(message = "La prioridad es obligatoria")
    private Prioridad prioridad;

    @NotNull(message = "El estado es obligatorio")
    private EstadoTicket estado;

    @NotBlank(message = "El equipo es obligatorio")
    private String equipoId;

    /** Correo del tecnico asignado (opcional). */
    private String tecnicoCorreo;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    @Size(max = 300)
    private String comentarioEstado;

    private LocalDateTime fechaCierre;

    @Size(max = 300)
    private String solucion;

    /** Solicitud de origen (la asigna el servidor al confirmar). */
    private String solicitudId;
}
