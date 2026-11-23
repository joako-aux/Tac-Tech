package com.example.tactech_backend.service;

import com.example.tactech_backend.dto.EmpresaDto;
import com.example.tactech_backend.entity.Empresa;
import com.example.tactech_backend.exception.RecursoNoEncontradoException;
import com.example.tactech_backend.exception.ReglaNegocioException;
import com.example.tactech_backend.repository.EmpresaRepository;
import com.example.tactech_backend.repository.UsuarioRepository;
import com.example.tactech_backend.validation.Rut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD de empresas cliente. Solo lo usa el administrador. */
@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<EmpresaDto> listar() {
        return empresaRepository.findAllByOrderByNombreAsc().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public EmpresaDto obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public EmpresaDto crear(EmpresaDto datos) {
        String rut = Rut.normalizar(datos.getRut());
        if (empresaRepository.existsByNombreIgnoreCase(datos.getNombre().trim())) {
            throw new ReglaNegocioException("Ya existe una empresa con ese nombre");
        }
        if (empresaRepository.existsByRut(rut)) {
            throw new ReglaNegocioException("Ya existe una empresa con ese RUT");
        }
        Empresa empresa = new Empresa();
        aplicar(empresa, datos, rut);
        return aDto(empresaRepository.save(empresa));
    }

    @Transactional
    public EmpresaDto actualizar(Long id, EmpresaDto datos) {
        Empresa empresa = buscar(id);
        String rut = Rut.normalizar(datos.getRut());
        if (empresaRepository.existsByNombreIgnoreCaseAndIdNot(datos.getNombre().trim(), id)) {
            throw new ReglaNegocioException("Ya existe otra empresa con ese nombre");
        }
        if (empresaRepository.existsByRutAndIdNot(rut, id)) {
            throw new ReglaNegocioException("Ya existe otra empresa con ese RUT");
        }
        aplicar(empresa, datos, rut);
        return aDto(empresaRepository.save(empresa));
    }

    @Transactional
    public void eliminar(Long id) {
        Empresa empresa = buscar(id);
        if (usuarioRepository.existsByEmpresa_Id(id)) {
            throw new ReglaNegocioException("La empresa tiene usuarios cliente; eliminalos o cambialos de empresa primero");
        }
        empresaRepository.delete(empresa);
    }

    Empresa buscar(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la empresa " + id));
    }

    private void aplicar(Empresa empresa, EmpresaDto datos, String rutNormalizado) {
        empresa.setNombre(datos.getNombre().trim());
        empresa.setRut(rutNormalizado);
        empresa.setDireccion(datos.getDireccion().trim());
        empresa.setTelefono(datos.getTelefono() == null || datos.getTelefono().isBlank() ? null : datos.getTelefono().trim());
    }

    private EmpresaDto aDto(Empresa e) {
        return new EmpresaDto(e.getId(), e.getNombre(), e.getRut(), e.getDireccion(), e.getTelefono());
    }
}
