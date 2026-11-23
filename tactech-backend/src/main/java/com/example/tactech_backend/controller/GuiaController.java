package com.example.tactech_backend.controller;

import com.example.tactech_backend.dto.GuiaDto;
import com.example.tactech_backend.service.GuiaService;
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

@RestController
@RequestMapping("/api/guias")
@RequiredArgsConstructor
public class GuiaController {

    private final GuiaService guiaService;

    @GetMapping
    public List<GuiaDto> listar(
            @RequestParam(required = false) String ticketId,
            @RequestParam(required = false) String tecnico
    ) {
        return guiaService.listar(ticketId, tecnico);
    }

    @GetMapping("/{id}")
    public GuiaDto obtener(@PathVariable String id) {
        return guiaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuiaDto crear(@Valid @RequestBody GuiaDto datos) {
        return guiaService.crear(datos);
    }

    /** Upsert por id: lo usa la sincronizacion de la app. */
    @PutMapping("/{id}")
    public GuiaDto guardar(@PathVariable String id, @Valid @RequestBody GuiaDto datos) {
        return guiaService.guardar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        guiaService.eliminar(id);
    }
}
