package com.example.tactech_backend.controller;

import com.example.tactech_backend.dto.UsuarioDto;
import com.example.tactech_backend.dto.UsuarioRequest;
import com.example.tactech_backend.entity.Rol;
import com.example.tactech_backend.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gestion de usuarios (tecnicos y administradores). */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /** /api/usuarios?rol=TECNICO para listar solo tecnicos (ej: al asignar un ticket). */
    @GetMapping
    public List<UsuarioDto> listar(@RequestParam(required = false) Rol rol) {
        return usuarioService.listar(rol);
    }

    @GetMapping("/{id}")
    public UsuarioDto obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crear(@Valid @RequestBody UsuarioRequest datos) {
        return usuarioService.crear(datos);
    }

    @PutMapping("/{id}")
    public UsuarioDto actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest datos) {
        return usuarioService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
    }
}
