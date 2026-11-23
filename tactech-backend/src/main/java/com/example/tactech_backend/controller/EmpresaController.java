package com.example.tactech_backend.controller;

import com.example.tactech_backend.dto.EmpresaDto;
import com.example.tactech_backend.service.EmpresaService;
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
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    @GetMapping
    public List<EmpresaDto> listar() {
        return empresaService.listar();
    }

    @GetMapping("/{id}")
    public EmpresaDto obtener(@PathVariable Long id) {
        return empresaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaDto crear(@Valid @RequestBody EmpresaDto datos) {
        return empresaService.crear(datos);
    }

    @PutMapping("/{id}")
    public EmpresaDto actualizar(@PathVariable Long id, @Valid @RequestBody EmpresaDto datos) {
        return empresaService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        empresaService.eliminar(id);
    }
}
