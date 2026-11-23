package com.example.tactech_backend.dto;

import com.example.tactech_backend.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos para crear o actualizar un usuario (lo usa el administrador). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato valido")
    private String correo;

    /** Obligatoria al crear; al actualizar, si viene vacia se mantiene la anterior. */
    @Size(min = 6, max = 60, message = "La clave debe tener entre 6 y 60 caracteres")
    private String clave;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;

    private Boolean activo;

    /** Obligatoria cuando el rol es CLIENTE (se controla en UsuarioService). */
    private Long empresaId;
}
