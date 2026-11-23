package com.example.tactech_backend.controller;

import com.example.tactech_backend.dto.EquipoDto;
import com.example.tactech_backend.service.EquipoService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/equipos")
@RequiredArgsConstructor
public class EquipoController {

    private final EquipoService equipoService;

    @GetMapping
    public List<EquipoDto> listar() {
        return equipoService.listar();
    }

    @GetMapping("/{id}")
    public EquipoDto obtener(@PathVariable String id) {
        return equipoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoDto crear(@Valid @RequestBody EquipoDto datos) {
        return equipoService.crear(datos);
    }

    /** Upsert por id: lo usa la sincronizacion de la app. */
    @PutMapping("/{id}")
    public EquipoDto guardar(@PathVariable String id, @Valid @RequestBody EquipoDto datos) {
        return equipoService.guardar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        equipoService.eliminar(id);
    }
}
