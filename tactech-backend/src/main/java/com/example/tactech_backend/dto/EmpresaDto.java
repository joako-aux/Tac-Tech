package com.example.tactech_backend.dto;

import com.example.tactech_backend.validation.RutValido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaDto {

    /** Lo asigna la base de datos; se ignora al crear. */
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
    private String nombre;

    @NotBlank(message = "El RUT es obligatorio")
    @RutValido
    private String rut;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(min = 5, max = 120, message = "La direccion debe tener entre 5 y 120 caracteres")
    private String direccion;

    @Pattern(regexp = "^$|^\\+?56\\d{9}$", message = "El telefono debe tener el formato +56912345678")
    private String telefono;
}
