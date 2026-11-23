package com.example.tactech_backend.dto;

import com.example.tactech_backend.entity.Prioridad;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lo envia el administrador para convertir una solicitud en ticket asignado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarSolicitudRequest {

    @NotBlank(message = "Debes asignar un tecnico")
    @Email(message = "El correo del tecnico no es valido")
    private String tecnicoCorreo;

    @NotNull(message = "La prioridad es obligatoria")
    private Prioridad prioridad;
}
