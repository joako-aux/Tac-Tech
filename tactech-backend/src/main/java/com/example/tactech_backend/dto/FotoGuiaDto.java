package com.example.tactech_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FotoGuiaDto {

    @NotBlank(message = "El id de la foto es obligatorio")
    @Size(max = 36)
    private String id;

    @NotBlank(message = "La uri de la foto es obligatoria")
    @Size(max = 500)
    private String uri;

    private LocalDateTime fechaCaptura;
}
