package com.example.tactech_backend.dto;

import com.example.tactech_backend.entity.Rol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta con los datos publicos de un usuario. Nunca incluye la clave. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDto {
    private Long id;
    private String nombre;
    private String correo;
    private Rol rol;
    private boolean activo;
    private Long empresaId;
    private String empresaNombre;
}
