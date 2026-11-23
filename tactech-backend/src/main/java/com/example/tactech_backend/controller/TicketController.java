package com.example.tactech_backend.controller;

import com.example.tactech_backend.dto.CambioEstadoRequest;
import com.example.tactech_backend.dto.ResumenDto;
import com.example.tactech_backend.dto.TicketDto;
import com.example.tactech_backend.service.TicketService;
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
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * Ejemplos: /api/tickets?estado=PENDIENTES  /api/tickets?tecnico=tecnico@tactech.cl
     *           /api/tickets?cliente=cliente@tactech.cl
     */
    @GetMapping
    public List<TicketDto> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tecnico,
            @RequestParam(required = false) String cliente
    ) {
        return ticketService.listar(estado, tecnico, cliente);
    }

    @GetMapping("/resumen")
    public ResumenDto resumen() {
        return ticketService.resumen();
    }

    @GetMapping("/{id}")
    public TicketDto obtener(@PathVariable String id) {
        return ticketService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDto crear(@Valid @RequestBody TicketDto datos) {
        return ticketService.crear(datos);
    }

    /** Upsert por id: lo usa la sincronizacion de la app. */
    @PutMapping("/{id}")
    public TicketDto guardar(@PathVariable String id, @Valid @RequestBody TicketDto datos) {
        return ticketService.guardar(id, datos);
    }

    /** Cambio de estado con reglas de negocio (409 si no se cumple alguna). */
    @PutMapping("/{id}/estado")
    public TicketDto cambiarEstado(@PathVariable String id, @Valid @RequestBody CambioEstadoRequest cambio) {
        return ticketService.cambiarEstado(id, cambio);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        ticketService.eliminar(id);
    }
}
