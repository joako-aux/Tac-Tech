package com.example.tactech_backend.service;

import com.example.tactech_backend.dto.CambioEstadoRequest;
import com.example.tactech_backend.dto.TicketDto;
import com.example.tactech_backend.entity.Equipo;
import com.example.tactech_backend.entity.EstadoTicket;
import com.example.tactech_backend.entity.Prioridad;
import com.example.tactech_backend.entity.Propiedad;
import com.example.tactech_backend.entity.Ticket;
import com.example.tactech_backend.exception.ReglaNegocioException;
import com.example.tactech_backend.repository.GuiaRepository;
import com.example.tactech_backend.repository.SolicitudRepository;
import com.example.tactech_backend.repository.TicketRepository;
import com.example.tactech_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Prueba de las reglas de negocio del servicio sin base de datos:
 * los repositorios se reemplazan por objetos simulados (Mockito).
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private GuiaRepository guiaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EquipoService equipoService;
    @Mock
    private SolicitudRepository solicitudRepository;

    @InjectMocks
    private TicketService ticketService;

    private Ticket ticket;

    @BeforeEach
    void preparar() {
        Equipo equipo = new Equipo("eq-1", "Split muro", "MSAFB-12", "MID-12345", Propiedad.PROPIO_DEL_CLIENTE);
        ticket = Ticket.builder()
                .id("t-1").numero(1).titulo("Equipo no enfria").descripcion("No baja de 26 grados")
                .cliente("Clinica").direccion("Av. Apoquindo 1234").prioridad(Prioridad.ALTA)
                .estado(EstadoTicket.TRABAJO_EN_PROCESO).equipo(equipo)
                .fechaCreacion(LocalDateTime.now()).fechaActualizacion(LocalDateTime.now())
                .build();
        when(ticketRepository.findById("t-1")).thenReturn(Optional.of(ticket));
    }

    @Test
    void noSePuedeCerrarSinGuias() {
        when(guiaRepository.countByTicket_Id("t-1")).thenReturn(0L);
        CambioEstadoRequest cambio = new CambioEstadoRequest(EstadoTicket.CERRADO, "Se cambio el capacitor");
        assertThrows(ReglaNegocioException.class, () -> ticketService.cambiarEstado("t-1", cambio));
    }

    @Test
    void seCierraConGuiaYGuardaLaSolucion() {
        when(guiaRepository.countByTicket_Id("t-1")).thenReturn(1L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

        TicketDto resultado = ticketService.cambiarEstado("t-1",
                new CambioEstadoRequest(EstadoTicket.CERRADO, "Se cambio el capacitor"));

        assertEquals(EstadoTicket.CERRADO, resultado.getEstado());
        assertEquals("Se cambio el capacitor", resultado.getSolucion());
        assertNotNull(resultado.getFechaCierre());
    }

    @Test
    void ticketTerminadoNoAdmiteCambios() {
        ticket.setEstado(EstadoTicket.CANCELADO);
        CambioEstadoRequest cambio = new CambioEstadoRequest(EstadoTicket.ABIERTO, "Reabrir ticket");
        assertThrows(ReglaNegocioException.class, () -> ticketService.cambiarEstado("t-1", cambio));
    }
}
