package com.example.tactech_backend.service;

import com.example.tactech_backend.dto.LoginRequest;
import com.example.tactech_backend.dto.UsuarioDto;
import com.example.tactech_backend.dto.UsuarioRequest;
import com.example.tactech_backend.entity.Empresa;
import com.example.tactech_backend.entity.Rol;
import com.example.tactech_backend.entity.Usuario;
import com.example.tactech_backend.exception.CredencialesInvalidasException;
import com.example.tactech_backend.exception.RecursoNoEncontradoException;
import com.example.tactech_backend.exception.ReglaNegocioException;
import com.example.tactech_backend.repository.SolicitudRepository;
import com.example.tactech_backend.repository.TicketRepository;
import com.example.tactech_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion total de usuarios (la hace el administrador): tecnicos, clientes y administradores.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TicketRepository ticketRepository;
    private final SolicitudRepository solicitudRepository;
    private final EmpresaService empresaService;
    private final PasswordEncoder passwordEncoder;

    /** @param rol filtro opcional: TECNICO, CLIENTE o ADMINISTRADOR */
    @Transactional(readOnly = true)
    public List<UsuarioDto> listar(Rol rol) {
        List<Usuario> usuarios = rol == null
                ? usuarioRepository.findAllByOrderByNombreAsc()
                : usuarioRepository.findByRolOrderByNombreAsc(rol);
        return usuarios.stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public UsuarioDto crear(UsuarioRequest datos) {
        if (datos.getClave() == null || datos.getClave().isBlank()) {
            throw new ReglaNegocioException("La clave es obligatoria al crear un usuario");
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(datos.getCorreo().trim())) {
            throw new ReglaNegocioException("Ya existe un usuario con el correo " + datos.getCorreo());
        }
        Usuario usuario = new Usuario();
        usuario.setActivo(true);
        aplicar(usuario, datos);
        usuario.setClave(passwordEncoder.encode(datos.getClave()));
        return aDto(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioDto actualizar(Long id, UsuarioRequest datos) {
        Usuario usuario = buscar(id);
        if (usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(datos.getCorreo().trim(), id)) {
            throw new ReglaNegocioException("Ya existe otro usuario con el correo " + datos.getCorreo());
        }
        aplicar(usuario, datos);
        // Solo se cambia la clave si viene una nueva
        if (datos.getClave() != null && !datos.getClave().isBlank()) {
            usuario.setClave(passwordEncoder.encode(datos.getClave()));
        }
        return aDto(usuarioRepository.save(usuario));
    }

    /** No se borra a quien tiene historial: se sugiere desactivarlo para no perder la trazabilidad. */
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = buscar(id);
        if (ticketRepository.existsByTecnico_Id(id) || solicitudRepository.existsByCliente_Id(id)) {
            throw new ReglaNegocioException("El usuario tiene tickets o solicitudes asociadas; desactivalo en vez de eliminarlo");
        }
        usuarioRepository.delete(usuario);
    }

    /** Compara la clave con el hash BCrypt. El mismo error para correo o clave mala: no revela cual fallo. */
    @Transactional(readOnly = true)
    public UsuarioDto login(LoginRequest datos) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(datos.getCorreo().trim())
                .filter(Usuario::isActivo)
                .orElseThrow(CredencialesInvalidasException::new);
        if (!passwordEncoder.matches(datos.getClave(), usuario.getClave())) {
            throw new CredencialesInvalidasException();
        }
        return aDto(usuario);
    }

    /** Tecnico activo con ese correo, o error 409. Lo usa la confirmacion de solicitudes. */
    @Transactional(readOnly = true)
    public Usuario buscarTecnicoActivo(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo.trim())
                .filter(u -> u.getRol() == Rol.TECNICO && u.isActivo())
                .orElseThrow(() -> new ReglaNegocioException("No existe un tecnico activo con el correo " + correo));
    }

    private void aplicar(Usuario usuario, UsuarioRequest datos) {
        usuario.setNombre(datos.getNombre().trim());
        usuario.setCorreo(datos.getCorreo().trim().toLowerCase());
        usuario.setRol(datos.getRol());
        if (datos.getActivo() != null) {
            usuario.setActivo(datos.getActivo());
        }
        // Regla: un cliente pertenece a una empresa; los demas roles no
        if (datos.getRol() == Rol.CLIENTE) {
            if (datos.getEmpresaId() == null) {
                throw new ReglaNegocioException("Un usuario cliente debe pertenecer a una empresa");
            }
            Empresa empresa = empresaService.buscar(datos.getEmpresaId());
            usuario.setEmpresa(empresa);
        } else {
            usuario.setEmpresa(null);
        }
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + id));
    }

    UsuarioDto aDto(Usuario u) {
        Empresa e = u.getEmpresa();
        return new UsuarioDto(
                u.getId(), u.getNombre(), u.getCorreo(), u.getRol(), u.isActivo(),
                e != null ? e.getId() : null,
                e != null ? e.getNombre() : null
        );
    }
}
