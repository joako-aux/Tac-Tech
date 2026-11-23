package com.example.tactech_backend.service;

import com.example.tactech_backend.dto.ConfirmarSolicitudRequest;
import com.example.tactech_backend.dto.SolicitudDto;
import com.example.tactech_backend.dto.TicketDto;
import com.example.tactech_backend.entity.Equipo;
import com.example.tactech_backend.entity.EstadoSolicitud;
import com.example.tactech_backend.entity.Solicitud;
import com.example.tactech_backend.entity.Ticket;
import com.example.tactech_backend.entity.Usuario;
import com.example.tactech_backend.exception.RecursoNoEncontradoException;
import com.example.tactech_backend.exception.ReglaNegocioException;
import com.example.tactech_backend.repository.EquipoRepository;
import com.example.tactech_backend.repository.SolicitudRepository;
import com.example.tactech_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Ciclo de vida de las solicitudes:
 * POR_CONFIRMAR --(cliente cancela)--> CANCELADA
 * POR_CONFIRMAR --(admin rechaza)----> RECHAZADA
 * POR_CONFIRMAR --(admin confirma)---> CONFIRMADA + ticket asignado a un tecnico
 * Una vez procesada, la solicitud ya no se puede modificar.
 */
@Service
@RequiredArgsConstructor
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipoRepository equipoRepository;
    private final UsuarioService usuarioService;
    private final TicketService ticketService;

    /**
     * @param estado  POR_CONFIRMAR, CONFIRMADA, RECHAZADA o CANCELADA (opcional)
     * @param cliente correo del cliente (opcional): solo sus solicitudes
     */
    @Transactional(readOnly = true)
    public List<SolicitudDto> listar(String estado, String cliente) {
        List<Solicitud> base = (cliente == null || cliente.isBlank())
                ? solicitudRepository.findAllByOrderByFechaCreacionDesc()
                : solicitudRepository.findByClienteCorreoIgnoreCaseOrderByFechaCreacionDesc(cliente.trim());
        if (estado == null || estado.isBlank()) {
            return base.stream().map(this::aDto).toList();
        }
        EstadoSolicitud buscado = Arrays.stream(EstadoSolicitud.values())
                .filter(e -> e.name().equalsIgnoreCase(estado.trim()))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("Estado de solicitud desconocido: " + estado));
        return base.stream().filter(s -> s.getEstado() == buscado).map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public SolicitudDto obtener(String id) {
        return aDto(buscar(id));
    }

    /**
     * Crea o actualiza una solicitud enviada por la app del cliente (sincronizacion).
     * El cliente solo puede dejarla en POR_CONFIRMAR o CANCELADA, y solo mientras no
     * haya sido procesada. Si el administrador ya la confirmo o rechazo, responde 409
     * y la app se queda con la version del servidor.
     */
    @Transactional
    public SolicitudDto guardarDesdeCliente(String id, SolicitudDto datos) {
        if (datos.getEstado() != EstadoSolicitud.POR_CONFIRMAR && datos.getEstado() != EstadoSolicitud.CANCELADA) {
            throw new ReglaNegocioException("El cliente solo puede crear o cancelar solicitudes");
        }
        Solicitud solicitud = solicitudRepository.findById(id).orElse(null);
        LocalDateTime ahora = LocalDateTime.now();
        if (solicitud == null) {
            solicitud = new Solicitud();
            solicitud.setId(id);
            solicitud.setFechaCreacion(datos.getFechaCreacion() != null ? datos.getFechaCreacion() : ahora);
        } else if (solicitud.getEstado() != EstadoSolicitud.POR_CONFIRMAR) {
            throw new ReglaNegocioException("La solicitud ya fue procesada y no se puede modificar");
        }
        solicitud.setClienteCorreo(datos.getClienteCorreo().trim().toLowerCase());
        solicitud.setCliente(usuarioRepository.findByCorreoIgnoreCase(datos.getClienteCorreo().trim()).orElse(null));
        solicitud.setTitulo(datos.getTitulo().trim());
        solicitud.setDescripcion(datos.getDescripcion().trim());
        solicitud.setDireccion(datos.getDireccion().trim());
        solicitud.setEquipoNombre(datos.getEquipoNombre().trim());
        solicitud.setEquipoModelo(datos.getEquipoModelo().trim());
        solicitud.setEquipoSerie(datos.getEquipoSerie().trim());
        solicitud.setPropiedad(datos.getPropiedad());
        solicitud.setEstado(datos.getEstado());
        solicitud.setMotivo(datos.getMotivo());
        solicitud.setFechaActualizacion(datos.getFechaActualizacion() != null ? datos.getFechaActualizacion() : ahora);
        return aDto(solicitudRepository.save(solicitud));
    }

    /** El cliente anula su solicitud por error (solo si sigue POR_CONFIRMAR). */
    @Transactional
    public SolicitudDto cancelar(String id, String motivo) {
        Solicitud solicitud = porConfirmar(id);
        solicitud.setEstado(EstadoSolicitud.CANCELADA);
        solicitud.setMotivo(motivo.trim());
        solicitud.setFechaActualizacion(LocalDateTime.now());
        return aDto(solicitudRepository.save(solicitud));
    }

    /** El administrador rechaza la solicitud con un motivo. */
    @Transactional
    public SolicitudDto rechazar(String id, String motivo) {
        Solicitud solicitud = porConfirmar(id);
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setMotivo(motivo.trim());
        solicitud.setFechaActualizacion(LocalDateTime.now());
        return aDto(solicitudRepository.save(solicitud));
    }

    /**
     * El administrador confirma: se registra (o reutiliza por serie) el equipo, se crea el
     * ticket formal asignado al tecnico y la solicitud pasa a CONFIRMADA. Todo en una transaccion.
     */
    @Transactional
    public TicketDto confirmar(String id, ConfirmarSolicitudRequest datos) {
        Solicitud solicitud = porConfirmar(id);
        Usuario tecnico = usuarioService.buscarTecnicoActivo(datos.getTecnicoCorreo());

        Equipo equipo = equipoRepository.findFirstBySerie(solicitud.getEquipoSerie())
                .orElseGet(() -> Equipo.builder().id(UUID.randomUUID().toString()).build());
        equipo.setNombre(solicitud.getEquipoNombre());
        equipo.setModelo(solicitud.getEquipoModelo());
        equipo.setSerie(solicitud.getEquipoSerie());
        equipo.setPropiedad(solicitud.getPropiedad());
        equipo = equipoRepository.save(equipo);

        Ticket ticket = ticketService.crearDesdeSolicitud(solicitud, equipo, tecnico, datos.getPrioridad());

        solicitud.setEstado(EstadoSolicitud.CONFIRMADA);
        solicitud.setTicketId(ticket.getId());
        solicitud.setMotivo(null);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        solicitudRepository.save(solicitud);

        return ticketService.aDto(ticket);
    }

    private Solicitud porConfirmar(String id) {
        Solicitud solicitud = buscar(id);
        if (solicitud.getEstado() != EstadoSolicitud.POR_CONFIRMAR) {
            throw new ReglaNegocioException("La solicitud ya esta " + solicitud.getEstado().name().toLowerCase().replace('_', ' '));
        }
        return solicitud;
    }

    private Solicitud buscar(String id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la solicitud " + id));
    }

    private SolicitudDto aDto(Solicitud s) {
        Usuario c = s.getCliente();
        return new SolicitudDto(
                s.getId(),
                s.getClienteCorreo(),
                c != null ? c.getNombre() : null,
                c != null && c.getEmpresa() != null ? c.getEmpresa().getNombre() : null,
                s.getTitulo(),
                s.getDescripcion(),
                s.getDireccion(),
                s.getEquipoNombre(),
                s.getEquipoModelo(),
                s.getEquipoSerie(),
                s.getPropiedad(),
                s.getEstado(),
                s.getMotivo(),
                s.getTicketId(),
                s.getFechaCreacion(),
                s.getFechaActualizacion()
        );
    }
}
