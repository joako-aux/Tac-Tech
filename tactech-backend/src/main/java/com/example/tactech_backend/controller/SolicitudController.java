package com.example.tactech_backend.controller;

import com.example.tactech_backend.dto.ConfirmarSolicitudRequest;
import com.example.tactech_backend.dto.MotivoRequest;
import com.example.tactech_backend.dto.SolicitudDto;
import com.example.tactech_backend.dto.TicketDto;
import com.example.tactech_backend.service.SolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService solicitudService;

    /** Ejemplos: /api/solicitudes?estado=POR_CONFIRMAR   /api/solicitudes?cliente=cliente@tactech.cl */
    @GetMapping
    public List<SolicitudDto> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String cliente
    ) {
        return solicitudService.listar(estado, cliente);
    }

    @GetMapping("/{id}")
    public SolicitudDto obtener(@PathVariable String id) {
        return solicitudService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudDto crear(@Valid @RequestBody SolicitudDto datos) {
        return solicitudService.guardarDesdeCliente(datos.getId(), datos);
    }

    /** Upsert por id desde la app del cliente (sincronizacion). 409 si ya fue procesada. */
    @PutMapping("/{id}")
    public SolicitudDto guardar(@PathVariable String id, @Valid @RequestBody SolicitudDto datos) {
        return solicitudService.guardarDesdeCliente(id, datos);
    }

    @PutMapping("/{id}/cancelar")
    public SolicitudDto cancelar(@PathVariable String id, @Valid @RequestBody MotivoRequest datos) {
        return solicitudService.cancelar(id, datos.getMotivo());
    }

    @PutMapping("/{id}/rechazar")
    public SolicitudDto rechazar(@PathVariable String id, @Valid @RequestBody MotivoRequest datos) {
        return solicitudService.rechazar(id, datos.getMotivo());
    }

    /** Crea el ticket formal asignado al tecnico. Responde con el ticket creado. */
    @PostMapping("/{id}/confirmar")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDto confirmar(@PathVariable String id, @Valid @RequestBody ConfirmarSolicitudRequest datos) {
        return solicitudService.confirmar(id, datos);
    }
}
