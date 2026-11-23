package com.example.tactech_backend.dto;

import com.example.tactech_backend.entity.EstadoTicket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioEstadoRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoTicket estado;

    @NotBlank(message = "El comentario es obligatorio")
    @Size(min = 5, max = 300, message = "El comentario debe tener entre 5 y 300 caracteres")
    private String comentario;
}
