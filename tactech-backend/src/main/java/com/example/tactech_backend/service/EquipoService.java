package com.example.tactech_backend.service;

import com.example.tactech_backend.dto.EquipoDto;
import com.example.tactech_backend.entity.Equipo;
import com.example.tactech_backend.exception.RecursoNoEncontradoException;
import com.example.tactech_backend.exception.ReglaNegocioException;
import com.example.tactech_backend.repository.EquipoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipoService {

    private final EquipoRepository equipoRepository;

    @Transactional(readOnly = true)
    public List<EquipoDto> listar() {
        return equipoRepository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public EquipoDto obtener(String id) {
        return aDto(buscar(id));
    }

    @Transactional
    public EquipoDto crear(EquipoDto datos) {
        if (equipoRepository.existsById(datos.getId())) {
            throw new ReglaNegocioException("Ya existe el equipo " + datos.getId());
        }
        return guardar(datos.getId(), datos);
    }

    /** Crea o actualiza (upsert). Es lo que usa la sincronizacion de la app. */
    @Transactional
    public EquipoDto guardar(String id, EquipoDto datos) {
        Equipo equipo = equipoRepository.findById(id).orElseGet(Equipo::new);
        equipo.setId(id);
        equipo.setNombre(datos.getNombre().trim());
        equipo.setModelo(datos.getModelo().trim());
        equipo.setSerie(datos.getSerie().trim());
        equipo.setPropiedad(datos.getPropiedad());
        return aDto(equipoRepository.save(equipo));
    }

    @Transactional
    public void eliminar(String id) {
        try {
            equipoRepository.delete(buscar(id));
            equipoRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("El equipo tiene tickets o guias asociados y no se puede eliminar");
        }
    }

    Equipo buscar(String id) {
        return equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el equipo " + id));
    }

    private EquipoDto aDto(Equipo e) {
        return new EquipoDto(e.getId(), e.getNombre(), e.getModelo(), e.getSerie(), e.getPropiedad());
    }
}
