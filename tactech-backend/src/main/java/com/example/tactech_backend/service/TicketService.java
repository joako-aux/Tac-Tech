package com.example.tactech_backend.service;

import com.example.tactech_backend.dto.CambioEstadoRequest;
import com.example.tactech_backend.dto.ResumenDto;
import com.example.tactech_backend.dto.TicketDto;
import com.example.tactech_backend.entity.Equipo;
import com.example.tactech_backend.entity.EstadoTicket;
import com.example.tactech_backend.entity.Prioridad;
import com.example.tactech_backend.entity.Solicitud;
import com.example.tactech_backend.entity.Ticket;
import com.example.tactech_backend.entity.Usuario;
import com.example.tactech_backend.exception.RecursoNoEncontradoException;
import com.example.tactech_backend.exception.ReglaNegocioException;
import com.example.tactech_backend.repository.GuiaRepository;
import com.example.tactech_backend.repository.SolicitudRepository;
import com.example.tactech_backend.repository.TicketRepository;
import com.example.tactech_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Logica de negocio de los tickets. El controlador solo recibe y responde;
 * las reglas (cerrar con guias, no tocar tickets finalizados) viven aqui.
 */
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final GuiaRepository guiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipoService equipoService;
    private final SolicitudRepository solicitudRepository;

    /**
     * @param estado  PENDIENTES, TERMINADOS o el nombre de un estado (opcional)
     * @param tecnico correo del tecnico (opcional): solo sus tickets asignados
     * @param cliente correo del cliente (opcional): tickets nacidos de sus solicitudes
     */
    @Transactional(readOnly = true)
    public List<TicketDto> listar(String estado, String tecnico, String cliente) {
        List<Ticket> base;
        if (tecnico != null && !tecnico.isBlank()) {
            base = ticketRepository.findByTecnicoCorreoIgnoreCaseOrderByFechaActualizacionDesc(tecnico.trim());
        } else if (cliente != null && !cliente.isBlank()) {
            List<String> idsSolicitudes = solicitudRepository
                    .findByClienteCorreoIgnoreCaseOrderByFechaCreacionDesc(cliente.trim())
                    .stream().map(Solicitud::getId).toList();
            base = idsSolicitudes.isEmpty()
                    ? List.of()
                    : ticketRepository.findBySolicitudIdInOrderByFechaActualizacionDesc(idsSolicitudes);
        } else {
            base = ticketRepository.findAllByOrderByFechaActualizacionDesc();
        }
        return base.stream().filter(filtroDeEstado(estado)).map(this::aDto).toList();
    }

    /**
     * Crea el ticket formal a partir de una solicitud confirmada por el administrador.
     * El numero correlativo lo asigna el servidor para que no se repita entre usuarios.
     */
    @Transactional
    public Ticket crearDesdeSolicitud(Solicitud solicitud, Equipo equipo, Usuario tecnico, Prioridad prioridad) {
        LocalDateTime ahora = LocalDateTime.now();
        Usuario cliente = solicitud.getCliente();
        String nombreCliente = cliente != null && cliente.getEmpresa() != null
                ? cliente.getEmpresa().getNombre()
                : (cliente != null ? cliente.getNombre() : solicitud.getClienteCorreo());
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID().toString())
                .numero(ticketRepository.maxNumero() + 1)
                .titulo(solicitud.getTitulo())
                .descripcion(solicitud.getDescripcion())
                .cliente(nombreCliente)
                .direccion(solicitud.getDireccion())
                .prioridad(prioridad)
                .estado(EstadoTicket.ABIERTO)
                .equipo(equipo)
                .tecnico(tecnico)
                .tecnicoCorreo(tecnico.getCorreo())
                .solicitudId(solicitud.getId())
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .comentarioEstado("Ticket creado desde la solicitud y asignado a " + tecnico.getNombre())
                .build();
        return ticketRepository.save(ticket);
    }

    private Predicate<Ticket> filtroDeEstado(String estado) {
        if (estado == null || estado.isBlank()) return t -> true;
        return switch (estado.trim().toUpperCase()) {
            case "PENDIENTES" -> t -> !t.getEstado().esFinal();
            case "TERMINADOS" -> t -> t.getEstado().esFinal();
            default -> {
                EstadoTicket buscado = Arrays.stream(EstadoTicket.values())
                        .filter(e -> e.name().equalsIgnoreCase(estado.trim()))
                        .findFirst()
                        .orElseThrow(() -> new ReglaNegocioException("Estado desconocido: " + estado));
                yield t -> t.getEstado() == buscado;
            }
        };
    }

    @Transactional(readOnly = true)
    public TicketDto obtener(String id) {
        return aDto(buscar(id));
    }

    @Transactional
    public TicketDto crear(TicketDto datos) {
        if (ticketRepository.existsById(datos.getId())) {
            throw new ReglaNegocioException("Ya existe el ticket " + datos.getId());
        }
        return guardar(datos.getId(), datos);
    }

    /**
     * Crea o actualiza (upsert) con el estado que trae la app. La regla de cerrar con guias
     * ya se valido en el telefono: aqui no se exige porque la guia se sincroniza despues del ticket.
     */
    @Transactional
    public TicketDto guardar(String id, TicketDto datos) {
        Ticket ticket = ticketRepository.findById(id).orElseGet(Ticket::new);
        LocalDateTime ahora = LocalDateTime.now();

        ticket.setId(id);
        ticket.setNumero(datos.getNumero());
        ticket.setTitulo(datos.getTitulo().trim());
        ticket.setDescripcion(datos.getDescripcion().trim());
        ticket.setCliente(datos.getCliente().trim());
        ticket.setDireccion(datos.getDireccion().trim());
        ticket.setPrioridad(datos.getPrioridad());
        ticket.setEstado(datos.getEstado());
        ticket.setEquipo(equipoService.buscar(datos.getEquipoId()));
        ticket.setTecnico(buscarTecnico(datos.getTecnicoCorreo()));
        ticket.setTecnicoCorreo(datos.getTecnicoCorreo() == null ? null : datos.getTecnicoCorreo().trim().toLowerCase());
        if (datos.getSolicitudId() != null) {
            ticket.setSolicitudId(datos.getSolicitudId());
        }
        if (ticket.getFechaCreacion() == null) {
            ticket.setFechaCreacion(datos.getFechaCreacion() != null ? datos.getFechaCreacion() : ahora);
        }
        ticket.setFechaActualizacion(datos.getFechaActualizacion() != null ? datos.getFechaActualizacion() : ahora);
        ticket.setComentarioEstado(datos.getComentarioEstado());
        ticket.setFechaCierre(datos.getFechaCierre());
        ticket.setSolucion(datos.getSolucion());
        return aDto(ticketRepository.save(ticket));
    }

    /** Cambio de estado hecho directamente contra el servidor (por ejemplo, el administrador). */
    @Transactional
    public TicketDto cambiarEstado(String id, CambioEstadoRequest cambio) {
        Ticket ticket = buscar(id);
        if (ticket.getEstado().esFinal()) {
            throw new ReglaNegocioException("El ticket ya esta " + ticket.getEstado().name().toLowerCase() + " y no admite cambios");
        }
        if (ticket.getEstado() == cambio.getEstado()) {
            throw new ReglaNegocioException("El ticket ya esta en ese estado");
        }
        if (cambio.getEstado() == EstadoTicket.CERRADO && guiaRepository.countByTicket_Id(id) == 0) {
            throw new ReglaNegocioException("Para cerrar el ticket debe existir al menos una guia de servicio");
        }
        LocalDateTime ahora = LocalDateTime.now();
        ticket.setEstado(cambio.getEstado());
        ticket.setComentarioEstado(cambio.getComentario().trim());
        ticket.setFechaActualizacion(ahora);
        if (cambio.getEstado().esFinal()) {
            ticket.setFechaCierre(ahora);
        }
        if (cambio.getEstado() == EstadoTicket.CERRADO) {
            ticket.setSolucion(cambio.getComentario().trim());
        }
        return aDto(ticketRepository.save(ticket));
    }

    @Transactional
    public void eliminar(String id) {
        Ticket ticket = buscar(id);
        if (guiaRepository.countByTicket_Id(id) > 0) {
            throw new ReglaNegocioException("El ticket tiene guias emitidas y no se puede eliminar");
        }
        ticketRepository.delete(ticket);
    }

    /** Totales para el panel del administrador: pendientes, terminados y conteo por estado. */
    @Transactional(readOnly = true)
    public ResumenDto resumen() {
        List<EstadoTicket> finales = Arrays.stream(EstadoTicket.values()).filter(EstadoTicket::esFinal).toList();
        List<EstadoTicket> abiertos = Arrays.stream(EstadoTicket.values()).filter(e -> !e.esFinal()).toList();
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoTicket e : EstadoTicket.values()) {
            porEstado.put(e.name(), ticketRepository.countByEstado(e));
        }
        return new ResumenDto(
                ticketRepository.count(),
                ticketRepository.countByEstadoIn(abiertos),
                ticketRepository.countByEstadoIn(finales),
                porEstado
        );
    }

    Ticket buscar(String id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el ticket " + id));
    }

    private Usuario buscarTecnico(String correo) {
        if (correo == null || correo.isBlank()) return null;
        return usuarioRepository.findByCorreoIgnoreCase(correo.trim()).orElse(null);
    }

    TicketDto aDto(Ticket t) {
        return new TicketDto(
                t.getId(),
                t.getNumero(),
                t.getTitulo(),
                t.getDescripcion(),
                t.getCliente(),
                t.getDireccion(),
                t.getPrioridad(),
                t.getEstado(),
                t.getEquipo().getId(),
                t.getTecnico() != null ? t.getTecnico().getCorreo() : t.getTecnicoCorreo(),
                t.getFechaCreacion(),
                t.getFechaActualizacion(),
                t.getComentarioEstado(),
                t.getFechaCierre(),
                t.getSolucion(),
                t.getSolicitudId()
        );
    }
}
