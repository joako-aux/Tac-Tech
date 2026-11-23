package com.example.tactech_backend.controller;

import com.example.tactech_backend.dto.LoginRequest;
import com.example.tactech_backend.dto.UsuarioDto;
import com.example.tactech_backend.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    /** 200 con el usuario y su rol, o 401 si las credenciales no son validas. */
    @PostMapping("/login")
    public UsuarioDto login(@Valid @RequestBody LoginRequest datos) {
        return usuarioService.login(datos);
    }
}
